package org.project.floodalert.notification.worker;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.enums.NotificationPriority;
import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.project.floodalert.notification.model.NotificationPreference;
import org.project.floodalert.notification.repository.NotificationPreferenceRepository;
import org.project.floodalert.notification.repository.NotificationRepository;
import org.project.floodalert.notification.service.FcmFailureHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FcmDispatchWorker {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FcmFailureHandler fcmFailureHandler;
    private final Executor virtualThreadExecutor;

    private static final String REDIS_FCM_TOKEN_PREFIX = "user:fcm_token:";
    private static final int BATCH_SIZE = 500;
    private static final int SUB_BATCH_SIZE = 100;
    private static final int RETRY_DELAY_MINUTES = 5;

    public FcmDispatchWorker(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            RedisTemplate<String, Object> redisTemplate,
            FcmFailureHandler fcmFailureHandler,
            @Qualifier("virtualThreadExecutor") Executor virtualThreadExecutor) {

        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.redisTemplate = redisTemplate;
        this.fcmFailureHandler = fcmFailureHandler;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }


    @Scheduled(fixedDelayString = "${app.notification.dispatch.fixed-delay:2000}")
    @Transactional
    public void dispatchPendingNotifications() {
        try {
            long startTime = System.currentTimeMillis();

            List<Notification> pendingNotifications = notificationRepository
                    .findTopByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING, BATCH_SIZE);

            if (pendingNotifications.isEmpty()) {
                return;
            }

            log.debug("[Virtual Thread] Bắt đầu dispatch {} notifications PENDING",
                    pendingNotifications.size());

            List<Notification> validNotifications = filterValidNotificationsParallel(pendingNotifications);

            if (validNotifications.isEmpty()) {
                log.warn("Không có notification nào có FCM token hợp lệ trong batch");
                markAsFailedDueToMissingToken(pendingNotifications);
                return;
            }

            sendToFirebaseParallel(validNotifications);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Hoàn tất dispatch batch trong {}ms (Virtual Threads)", duration);

        } catch (Exception e) {
            log.error("Lỗi trong dispatchPendingNotifications", e);
        }
    }


    private List<Notification> filterValidNotificationsParallel(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("Bắt đầu lấy FCM token theo Batch cho {} notifications", notifications.size());

        // Lấy danh sách các userId duy nhất
        List<UUID> uniqueUserIds = notifications.stream()
                .map(Notification::getUserId)
                .distinct()
                .toList();

        List<String> redisKeys = uniqueUserIds.stream()
                .map(id -> REDIS_FCM_TOKEN_PREFIX + id.toString())
                .toList();

        Map<UUID, String> userTokenMap = new HashMap<>();
        List<UUID> missingFromRedis = new ArrayList<>(uniqueUserIds);

        try {
            List<Object> cachedTokens = redisTemplate.opsForValue().multiGet(redisKeys);

            if (cachedTokens != null) {
                missingFromRedis = new ArrayList<>();
                for (int i = 0; i < uniqueUserIds.size(); i++) {
                    Object tokenObj = cachedTokens.get(i);
                    if (tokenObj != null && !tokenObj.toString().isBlank()) {
                        userTokenMap.put(uniqueUserIds.get(i), tokenObj.toString());
                    } else {
                        missingFromRedis.add(uniqueUserIds.get(i));
                    }
                }
                log.debug("Redis MGET thành công. Tìm thấy {}/{} tokens từ cache, {} cần fallback DB.",
                        userTokenMap.size(), uniqueUserIds.size(), missingFromRedis.size());
            }

        } catch (Exception redisException) {
            log.warn("[CIRCUIT BREAKER] Lỗi kết nối Redis khi gọi MGET. Fallback xuống Database cho {} users. Chi tiết: {}",
                    uniqueUserIds.size(), redisException.getMessage());
        }

        if (!missingFromRedis.isEmpty()) {
            try {
                List<NotificationPreference> dbPrefs = preferenceRepository.findByUserIdIn(missingFromRedis);
                for (NotificationPreference pref : dbPrefs) {
                    String dbToken = pref.getFcmToken();
                    if (dbToken != null && !dbToken.isBlank()) {
                        userTokenMap.put(pref.getUserId(), dbToken);
                    }
                }
                log.debug("Fallback DB - Lấy thêm {} tokens từ Database (tổng: {}/{}).",
                        dbPrefs.size(), userTokenMap.size(), uniqueUserIds.size());
            } catch (Exception dbException) {
                log.error("Lỗi thảm họa: Fallback DB thất bại khi lấy batch tokens: {}", dbException.getMessage());
            }
        }

        // Gắn Token vào Entity
        List<Notification> validNotifications = new ArrayList<>();
        for (Notification notification : notifications) {
            String token = userTokenMap.get(notification.getUserId());
            if (token != null) {
                notification.setFcmToken(token);
                validNotifications.add(notification);
            }
        }

        return validNotifications;
    }

    private void sendToFirebaseParallel(List<Notification> notifications) {
        log.debug("Sending {} notifications to Firebase với sub-batch parallel processing",
                notifications.size());

        List<List<Notification>> subBatches = partitionList(notifications, SUB_BATCH_SIZE);

        log.debug("Chia thành {} sub-batches để xử lý parallel", subBatches.size());

        List<CompletableFuture<Void>> futures = subBatches.stream()
                .map(subBatch -> CompletableFuture.runAsync(() -> {
                    try {
                        sendSubBatchToFirebase(subBatch);
                    } catch (Exception e) {
                        log.error("Lỗi xử lý sub-batch", e);
                        markAllAsFailed(subBatch, "Sub-batch error: " + e.getMessage());
                    }
                }, virtualThreadExecutor))
                .toList();

        // Wait for all sub-batches to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.debug("Hoàn tất gửi tất cả sub-batches");
    }

    /**
     * Gửi một sub-batch đến Firebase
     */
    private void sendSubBatchToFirebase(List<Notification> subBatch) {
        log.debug("Processing sub-batch of {} notifications", subBatch.size());

        List<Message> messages = buildFirebaseMessages(subBatch);

        try {
            BatchResponse batchResponse = FirebaseMessaging.getInstance()
                    .sendEachAsync(messages)
                    .get();

            log.debug("Firebase sub-batch response: success={}, failure={}",
                    batchResponse.getSuccessCount(), batchResponse.getFailureCount());

            // Xử lý kết quả và cập nhật status
            processBatchResponse(subBatch, batchResponse);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread bị interrupt khi gửi FCM sub-batch", e);
            markAllAsFailed(subBatch, "Thread interrupted");

        } catch (ExecutionException e) {
            log.error("Lỗi khi gửi FCM sub-batch", e);
            markAllAsFailed(subBatch, "FCM execution error: " + e.getMessage());
        }
    }


    private <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }

    private void markAsFailedDueToMissingToken(List<Notification> notifications) {
        List<Notification> toUpdate = new ArrayList<>();

        for (Notification notification : notifications) {
            if (notification.getFcmToken() == null || notification.getFcmToken().isBlank()) {
                // Lưu với DELIVERED để vẫn hiện trong hộp thư, không đánh dấu retry
                notification.setStatus(NotificationStatus.DELIVERED);
                toUpdate.add(notification);
            }
        }

        if (!toUpdate.isEmpty()) {
            notificationRepository.saveAll(toUpdate);
            log.info("Đã lưu {} notifications với DELIVERED do thiếu FCM token (vẫn hiển thị trong hộp thư)", toUpdate.size());
        }
    }

    private List<Message> buildFirebaseMessages(List<Notification> notifications) {
        List<Message> messages = new ArrayList<>();

        for (Notification notification : notifications) {
            Message message = Message.builder()
                    .setToken(notification.getFcmToken())
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle(notification.getTitle())
                                    .setBody(notification.getBody())
                                    .build()
                    )
                    .putAllData(convertDataMapToStringMap(notification.getData()))
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(mapPriority(notification.getPriority()))
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .build())
                            .build())
                    .build();

            messages.add(message);
        }

        return messages;
    }


    private Map<String, String> convertDataMapToStringMap(Map<String, Object> data) {
        if (data == null) {
            return Map.of();
        }

        return data.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() != null ? e.getValue().toString() : ""
                ));
    }


    private AndroidConfig.Priority mapPriority(NotificationPriority priority) {
        if (priority == null) {
            return AndroidConfig.Priority.NORMAL;
        }

        return switch (priority) {
            case HIGH -> AndroidConfig.Priority.HIGH;
            case NORMAL -> AndroidConfig.Priority.NORMAL;
            case LOW -> AndroidConfig.Priority.NORMAL;
            case CRITICAL -> null;
        };
    }

    /**
     * Xử lý batch response từ Firebase
     */
    private void processBatchResponse(List<Notification> notifications, BatchResponse batchResponse) {
        List<Notification> toUpdate = new ArrayList<>();
        List<SendResponse> responses = batchResponse.getResponses();

        for (int i = 0; i < responses.size(); i++) {
            Notification notification = notifications.get(i);
            SendResponse response = responses.get(i);

            if (response.isSuccessful()) {
                // Thành công
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notification.setFcmMessageId(response.getMessageId());
                notification.setErrorMessage(null);
                log.debug("Gửi thành công notification {} cho user {}",
                        notification.getId(), notification.getUserId());

            } else {
                // Thất bại
                fcmFailureHandler.handleFailure(notification, response.getException());
            }

            toUpdate.add(notification);
        }

        // Batch update
        if (!toUpdate.isEmpty()) {
            notificationRepository.saveAll(toUpdate);
            log.debug("Đã cập nhật status cho {} notifications", toUpdate.size());
        }
    }

    /**
     * Mark tất cả notifications thành FAILED (fallback khi có lỗi)
     */
    private void markAllAsFailed(List<Notification> notifications, String errorMessage) {
        for (Notification notification : notifications) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setErrorMessage(errorMessage);
            notification.setNextRetryAt(LocalDateTime.now().plusMinutes(RETRY_DELAY_MINUTES));
        }

        notificationRepository.saveAll(notifications);
        log.warn("Đã mark {} notifications thành FAILED: {}", notifications.size(), errorMessage);
    }
}
