package org.project.floodalert.notification.worker;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.project.floodalert.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NotificationRetryWorker {

    private final NotificationRepository notificationRepository;
    private final @Qualifier("virtualThreadExecutor") Executor virtualThreadExecutor;

    public NotificationRetryWorker(NotificationRepository notificationRepository,
                                   @Qualifier("virtualThreadExecutor") Executor virtualThreadExecutor) {
        this.notificationRepository = notificationRepository;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @Value("${app.notification.retry.max-retries:3}")
    private int maxRetries;

    @Value("${app.notification.cleanup.read-retention-days:30}")
    private int readRetentionDays;

    @Value("${app.notification.cleanup.failed-retention-days:15}")
    private int failedRetentionDays;

    @Scheduled(cron = "${app.notification.retry.cron:0 * * * * *}")
    @Transactional
    public void processFailedNotifications() {
        try {

            List<Notification> failedNotifications = notificationRepository.findReadyForRetry(
                    NotificationStatus.FAILED,
                    maxRetries,
                    LocalDateTime.now()
            );

            if (failedNotifications.isEmpty()) {
                return;
            }

            log.info("[Virtual Thread] Bắt đầu xử lý {} notifications FAILED đến hạn retry",
                    failedNotifications.size());

            Map<Boolean, List<Notification>> classified = classifyNotificationsParallel(failedNotifications);

            List<Notification> toRetry = classified.get(true);
            List<Notification> toDead = classified.get(false);

            List<Notification> allUpdates = new ArrayList<>();
            if (toRetry != null) allUpdates.addAll(toRetry);
            if (toDead != null) allUpdates.addAll(toDead);

            if (!allUpdates.isEmpty()) {
                notificationRepository.saveAll(allUpdates);

                log.info("Đã cập nhật status: {} reset về PENDING, {}",
                        toRetry != null ? toRetry.size() : 0,
                        toDead != null ? toDead.size() : 0);

                if (toDead != null && !toDead.isEmpty()) {
                    logDeadNotificationsAsync(toDead);
                }
            }

        } catch (Exception e) {
            log.error("Lỗi trong processFailedNotifications", e);
        }
    }

    private Map<Boolean, List<Notification>> classifyNotificationsParallel(
            List<Notification> failedNotifications) {

        log.debug(" Classifying {} notifications parallel", failedNotifications.size());

        List<CompletableFuture<Map.Entry<Boolean, Notification>>> futures =
                failedNotifications.stream()
                        .map(notification -> CompletableFuture.supplyAsync(() -> {
                            boolean shouldRetry = shouldRetry(notification);

                            if (shouldRetry) {
                                prepareForRetry(notification);
                            } else {
                                markAsDead(notification);
                            }

                            return Map.entry(shouldRetry, notification);
                        }, virtualThreadExecutor))
                        .toList();

        Map<Boolean, List<Notification>> result = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        log.debug("Classification completed");
        return result;
    }

    /**
     * Kiểm tra xem notification có nên retry không
     */
    private boolean shouldRetry(Notification notification) {
        int currentRetryCount = notification.getRetryCount() != null ? notification.getRetryCount() : 0;
        int maxRetriesForNotification = notification.getMaxRetries() != null ? notification.getMaxRetries() : maxRetries;

        return currentRetryCount < maxRetriesForNotification;
    }


    private void prepareForRetry(Notification notification) {
        notification.setStatus(NotificationStatus.PENDING);
        notification.setNextRetryAt(null);

        log.debug("Reset notification {} về PENDING (retry {}/{})",
                notification.getId(),
                notification.getRetryCount(),
                notification.getMaxRetries());
    }

    /**
     * Mark notification thành DEAD khi vượt quá max retries
     */
    private void markAsDead(Notification notification) {
        notification.setStatus(NotificationStatus.DEAD);
        notification.setErrorMessage(
                String.format("Exceeded max retries (%d/%d). Original error: %s",
                        notification.getRetryCount(),
                        notification.getMaxRetries(),
                        notification.getErrorMessage())
        );

        log.warn(" Mark notification {} thành DEAD (retried {}/{} times) - User: {}",
                notification.getId(),
                notification.getRetryCount(),
                notification.getMaxRetries(),
                notification.getUserId());
    }

    private void logDeadNotificationsAsync(List<Notification> deadNotifications) {
        CompletableFuture.runAsync(() -> {
            try {
                log.warn("Total dead notifications: {}", deadNotifications.size());

                List<CompletableFuture<Void>> logFutures = deadNotifications.stream()
                        .map(notification -> CompletableFuture.runAsync(() -> {
                            log.warn("[DEAD] ID: {}, User: {}, Created: {}, RetryCount: {}, Error: {}",
                                    notification.getId(),
                                    notification.getUserId(),
                                    notification.getCreatedAt(),
                                    notification.getRetryCount(),
                                    notification.getErrorMessage());
                        }, virtualThreadExecutor))
                        .toList();

                CompletableFuture.allOf(logFutures.toArray(new CompletableFuture[0])).join();

            } catch (Exception e) {
                log.error("Lỗi khi log DEAD notifications", e);
            }
        }, virtualThreadExecutor);
    }


    /**
     * Dọn dẹp định kỳ: xóa notification đã đọc (CLICKED) quá {@code readRetentionDays} ngày
     * (tính theo thời điểm đọc) và notification gửi lỗi (FAILED/DEAD) quá {@code failedRetentionDays}
     * ngày (tính theo thời điểm tạo).
     */
    @Scheduled(cron = "${app.notification.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void cleanupOldNotifications() {
        try {
            long startTime = System.currentTimeMillis();
            LocalDateTime now = LocalDateTime.now();

            int deletedRead = notificationRepository.deleteByStatusAndClickedAtBefore(
                    NotificationStatus.CLICKED,
                    now.minusDays(readRetentionDays)
            );

            int deletedFailed = notificationRepository.deleteByStatusInAndCreatedAtBefore(
                    List.of(NotificationStatus.FAILED, NotificationStatus.DEAD),
                    now.minusDays(failedRetentionDays)
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("[Cleanup] Đã xóa {} notification đã đọc (>{} ngày) và {} notification gửi lỗi (>{} ngày) trong {}ms",
                    deletedRead, readRetentionDays, deletedFailed, failedRetentionDays, duration);

        } catch (Exception e) {
            log.error("[Cleanup] Lỗi khi cleanup old notifications", e);
        }
    }
}
