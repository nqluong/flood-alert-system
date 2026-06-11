package org.project.floodalert.notification.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.project.floodalert.notification.model.NotificationPreference;
import org.project.floodalert.notification.repository.NotificationPreferenceRepository;
import org.project.floodalert.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmDispatchService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationRepository notificationRepository;
    private final FcmFailureHandler fcmFailureHandler;

    private static final int FCM_BATCH_SIZE = 500;
    private static final int RETRY_DELAY_MINUTES = 5;

    @Value("${app.notification.fcm.parallelism:8}")
    private int fcmParallelism;

    private ExecutorService fcmExecutor;

    @PostConstruct
    void initExecutor() {
        this.fcmExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("fcm-dispatch-", 0).factory()
        );
        log.info("[FCM] Khởi tạo Virtual Thread executor cho FCM dispatch (parallelism guideline={})", fcmParallelism);
    }

    @PreDestroy
    void shutdownExecutor() {
        if (fcmExecutor != null) {
            fcmExecutor.shutdown();
        }
    }

    @Transactional
    public int sendBatch(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            log.debug("[FCM] Không có notifications để gửi");
            return 0;
        }

        Map<UUID, String> userTokenMap = fetchFcmTokens(notifications);

        if (userTokenMap.isEmpty()) {
            log.info("[FCM] Không tìm thấy FCM token cho {} users — lưu DB để xem sau", notifications.size());
            markAllAsDeliveredWithoutPush(notifications);
            notificationRepository.saveAll(notifications);
            return 0;
        }

        List<Notification> validNotifications = assignTokensAndFilter(notifications, userTokenMap);
        if (validNotifications.isEmpty()) {
            log.warn("[FCM] Không có notification hợp lệ sau khi gán token");
            notificationRepository.saveAll(notifications);
            return 0;
        }

        int successCount = sendToFirebase(validNotifications);

        // Chỉ save 1 lần ở cuối, sau khi đã cập nhật status/messageId/sentAt.
        notificationRepository.saveAll(notifications);

        log.debug("[FCM] Hoàn tất batch: {}/{} thành công", successCount, validNotifications.size());
        return successCount;
    }

    /**
     * Fetch FCM tokens bằng 1 batch query, return userId → token map.
     */
    private Map<UUID, String> fetchFcmTokens(List<Notification> notifications) {
        Set<UUID> userIds = notifications.stream()
                .map(Notification::getUserId)
                .collect(Collectors.toSet());

        return preferenceRepository.findAllById(userIds).stream()
                .filter(pref -> pref.getFcmToken() != null && !pref.getFcmToken().isBlank())
                .collect(Collectors.toMap(
                        NotificationPreference::getUserId,
                        NotificationPreference::getFcmToken
                ));
    }

    /**
     * Gán token và phân tách valid/invalid bằng stream.partitioningBy. Bỏ vòng for thủ công.
     */
    private List<Notification> assignTokensAndFilter(List<Notification> notifications,
                                                    Map<UUID, String> userTokenMap) {
        Map<Boolean, List<Notification>> partitioned = notifications.stream()
                .peek(n -> n.setFcmToken(userTokenMap.get(n.getUserId())))
                .collect(Collectors.partitioningBy(
                        n -> n.getFcmToken() != null && !n.getFcmToken().isBlank()
                ));

        List<Notification> invalid = partitioned.get(false);
        if (!invalid.isEmpty()) {
            markAllAsDeliveredWithoutPush(invalid);
            log.info("[FCM] {} notifications thiếu FCM token — đánh dấu DELIVERED để xem trong hộp thư", invalid.size());
        }

        return partitioned.get(true);
    }

    private int sendToFirebase(List<Notification> notifications) {
        List<List<Notification>> batches = partitionList(notifications, FCM_BATCH_SIZE);
        if (batches.size() == 1) {
            return sendSingleBatch(batches.get(0));
        }

        log.debug("[FCM] Gửi {} batches song song", batches.size());

        List<CompletableFuture<Integer>> futures = batches.stream()
                .map(batch -> CompletableFuture.supplyAsync(() -> sendSingleBatch(batch), fcmExecutor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .mapToInt(CompletableFuture::join)
                .sum();
    }

    private int sendSingleBatch(List<Notification> batch) {
        try {
            List<Message> messages = batch.stream()
                    .map(this::buildFirebaseMessage)
                    .collect(Collectors.toList());

            BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);

            applyBatchResponse(batch, response);

            log.debug("[FCM] Batch {} msgs: {} success, {} failed",
                    batch.size(), response.getSuccessCount(), response.getFailureCount());
            return response.getSuccessCount();

        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Lỗi Firebase khi gửi batch ({} msgs)", batch.size(), e);
            markBatchAsFailed(batch, "Firebase error: " + e.getMessage());
            return 0;
        } catch (Exception e) {
            log.error("[FCM] Lỗi không xác định khi gửi batch ({} msgs)", batch.size(), e);
            markBatchAsFailed(batch, "Unexpected error: " + e.getMessage());
            return 0;
        }
    }

    private void applyBatchResponse(List<Notification> batch, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        LocalDateTime now = LocalDateTime.now();

        IntStream.range(0, responses.size()).forEach(i -> {
            SendResponse sr = responses.get(i);
            Notification n = batch.get(i);
            if (sr.isSuccessful()) {
                n.setStatus(NotificationStatus.SENT);
                n.setFcmMessageId(sr.getMessageId());
                n.setSentAt(now);
            } else {
                fcmFailureHandler.handleFailure(n, sr.getException());
            }
        });
    }

    private Message buildFirebaseMessage(Notification notification) {
        com.google.firebase.messaging.Notification fcmNotification =
                com.google.firebase.messaging.Notification.builder()
                        .setTitle(notification.getTitle())
                        .setBody(notification.getBody())
                        .build();

        Map<String, String> data = new HashMap<>();
        if (notification.getData() != null) {
            notification.getData().forEach((k, v) -> data.put(k, v != null ? v.toString() : ""));
        }
        if (notification.getId() != null) {
            data.put("notificationId", notification.getId().toString());
        }
        if (notification.getNotificationType() != null) {
            data.put("notificationType", notification.getNotificationType());
        }

        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setSound("default")
                        .setChannelId("flood_alerts")
                        .build())
                .build();

        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound("default")
                        .setBadge(1)
                        .build())
                .build();

        return Message.builder()
                .setToken(notification.getFcmToken())
                .setNotification(fcmNotification)
                .putAllData(data)
                .setAndroidConfig(androidConfig)
                .setApnsConfig(apnsConfig)
                .build();
    }

    private void markBatchAsFailed(List<Notification> batch, String errorMessage) {
        LocalDateTime retryAt = LocalDateTime.now().plusMinutes(RETRY_DELAY_MINUTES);
        batch.forEach(n -> {
            n.setStatus(NotificationStatus.FAILED);
            n.setErrorMessage(errorMessage);
            n.setRetryCount(n.getRetryCount() + 1);
            n.setNextRetryAt(retryAt);
        });
    }

    private void markAllAsFailed(List<Notification> notifications, String errorMessage) {
        notifications.forEach(n -> {
            n.setStatus(NotificationStatus.FAILED);
            n.setErrorMessage(errorMessage);
        });
    }

    private void markAllAsDeliveredWithoutPush(List<Notification> notifications) {
        notifications.forEach(n -> n.setStatus(NotificationStatus.DELIVERED));
    }

    private <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
        if (list.isEmpty()) return Collections.emptyList();
        if (list.size() <= partitionSize) return List.of(list);

        int total = (list.size() + partitionSize - 1) / partitionSize;
        List<List<T>> partitions = new ArrayList<>(total);
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }
}
