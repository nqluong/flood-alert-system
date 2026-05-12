package org.project.floodalert.notification.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.project.floodalert.notification.model.NotificationPreference;
import org.project.floodalert.notification.repository.NotificationPreferenceRepository;
import org.project.floodalert.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmDispatchService {
    
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationRepository notificationRepository;
    
    private static final int FCM_BATCH_SIZE = 500; // Firebase limit
    
    /**
     * Gửi batch notifications qua FCM
     * 
     * @param notifications Danh sách notifications cần gửi
     * @return Số lượng notifications gửi thành công
     */
    @Transactional
    public int sendBatch(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            log.warn("Không có notifications nào để gửi");
            return 0;
        }
        
        log.debug("Bắt đầu gửi batch {} notifications", notifications.size());
        
        // Bước 1: Fetch FCM tokens cho tất cả users
        Map<UUID, String> userTokenMap = fetchFcmTokens(notifications);
        
        if (userTokenMap.isEmpty()) {
            log.warn("Không tìm thấy FCM token nào cho {} users", notifications.size());
            markAllAsFailed(notifications, "No FCM tokens found");
            return 0;
        }
        
        // Bước 2: Gán token vào notifications và lọc những notification hợp lệ
        List<Notification> validNotifications = assignTokensAndFilter(notifications, userTokenMap);
        
        if (validNotifications.isEmpty()) {
            log.warn("Không có notification hợp lệ nào sau khi gán token");
            return 0;
        }
        
        log.debug("Có {} notifications hợp lệ (có FCM token) từ {} notifications ban đầu",
                validNotifications.size(), notifications.size());
        
        // Bước 3: Save notifications để generate ID (cần cho buildFirebaseMessage)
        notificationRepository.saveAll(notifications);
        log.debug("Đã save {} notifications vào database (generate IDs)", notifications.size());
        
        // Bước 4: Gửi batch qua Firebase
        int successCount = sendToFirebase(validNotifications);
        
        // Bước 5: Lưu lại kết quả (update status)
        notificationRepository.saveAll(notifications);
        
        log.debug("Hoàn tất gửi batch: {}/{} thành công", successCount, validNotifications.size());
        
        return successCount;
    }
    
    /**
     * Fetch FCM tokens cho tất cả users trong batch
     */
    private Map<UUID, String> fetchFcmTokens(List<Notification> notifications) {
        Set<UUID> userIds = notifications.stream()
                .map(Notification::getUserId)
                .collect(Collectors.toSet());
        
        log.debug("Fetching FCM tokens cho {} unique users", userIds.size());
        
        List<NotificationPreference> preferences = preferenceRepository.findAllById(userIds);
        
        Map<UUID, String> tokenMap = preferences.stream()
                .filter(pref -> pref.getFcmToken() != null && !pref.getFcmToken().isBlank())
                .collect(Collectors.toMap(
                        NotificationPreference::getUserId,
                        NotificationPreference::getFcmToken
                ));
        
        log.debug("Tìm thấy {} FCM tokens từ {} users", tokenMap.size(), userIds.size());
        
        return tokenMap;
    }
    
    /**
     * Gán FCM token vào notifications và lọc những notification hợp lệ
     */
    private List<Notification> assignTokensAndFilter(
            List<Notification> notifications,
            Map<UUID, String> userTokenMap) {
        
        List<Notification> validNotifications = new ArrayList<>();
        
        for (Notification notification : notifications) {
            String token = userTokenMap.get(notification.getUserId());
            
            if (token != null && !token.isBlank()) {
                notification.setFcmToken(token);
                validNotifications.add(notification);
            } else {
                // Đánh dấu failed nếu không có token
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage("Missing FCM token");
                log.debug("User {} không có FCM token", notification.getUserId());
            }
        }
        
        return validNotifications;
    }
    
    /**
     * Gửi notifications qua Firebase Admin SDK
     * Chia thành các batch nhỏ nếu vượt quá limit
     */
    private int sendToFirebase(List<Notification> notifications) {
        int totalSuccess = 0;
        
        // Chia thành các batch nhỏ (Firebase limit 500 messages/batch)
        List<List<Notification>> batches = partitionList(notifications, FCM_BATCH_SIZE);
        
        log.debug("Chia thành {} batches để gửi (batch size: {})", batches.size(), FCM_BATCH_SIZE);
        
        for (int i = 0; i < batches.size(); i++) {
            List<Notification> batch = batches.get(i);
            log.debug("Gửi batch {}/{} với {} messages", i + 1, batches.size(), batch.size());
            
            int batchSuccess = sendSingleBatch(batch);
            totalSuccess += batchSuccess;
        }
        
        return totalSuccess;
    }
    
    /**
     * Gửi một batch đơn qua Firebase
     */
    private int sendSingleBatch(List<Notification> batch) {
        try {
            // Build Firebase messages
            List<Message> messages = batch.stream()
                    .map(this::buildFirebaseMessage)
                    .collect(Collectors.toList());
            
            // Gửi batch
            BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);
            
            int successCount = response.getSuccessCount();
            int failureCount = response.getFailureCount();
            
            log.debug("Firebase batch response: {} success, {} failed", successCount, failureCount);
            
            // Xử lý kết quả từng message
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse sendResponse = responses.get(i);
                Notification notification = batch.get(i);
                
                if (sendResponse.isSuccessful()) {
                    notification.setStatus(NotificationStatus.SENT);
                    notification.setFcmMessageId(sendResponse.getMessageId());
                    notification.setSentAt(LocalDateTime.now());
                    log.debug("Gửi thành công notification {} cho user {}", 
                            notification.getId(), notification.getUserId());
                } else {
                    FirebaseMessagingException exception = sendResponse.getException();
                    notification.setStatus(NotificationStatus.FAILED);
                    notification.setErrorMessage(exception != null ? exception.getMessage() : "Unknown error");
                    log.warn("Gửi thất bại notification {} cho user {}: {}", 
                            notification.getId(), notification.getUserId(), 
                            notification.getErrorMessage());
                }
            }
            
            return successCount;
            
        } catch (FirebaseMessagingException e) {
            log.error("Lỗi khi gửi batch qua Firebase", e);
            markBatchAsFailed(batch, "Firebase error: " + e.getMessage());
            return 0;
        } catch (Exception e) {
            log.error("Lỗi không xác định khi gửi batch", e);
            markBatchAsFailed(batch, "Unexpected error: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Build Firebase Message từ Notification
     */
    private Message buildFirebaseMessage(Notification notification) {
        // Build notification payload
        com.google.firebase.messaging.Notification fcmNotification = 
                com.google.firebase.messaging.Notification.builder()
                        .setTitle(notification.getTitle())
                        .setBody(notification.getBody())
                        .build();
        
        // Build data payload
        Map<String, String> data = new HashMap<>();
        if (notification.getData() != null) {
            notification.getData().forEach((key, value) -> 
                    data.put(key, value != null ? value.toString() : ""));
        }
        
        // Thêm notificationId nếu có (sau khi save)
        if (notification.getId() != null) {
            data.put("notificationId", notification.getId().toString());
        }
        
        if (notification.getNotificationType() != null) {
            data.put("notificationType", notification.getNotificationType());
        }
        
        // Build Android config
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setSound("default")
                        .setChannelId("flood_alerts")
                        .build())
                .build();
        
        // Build APNS config (iOS)
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
    
    /**
     * Đánh dấu tất cả notifications trong batch là failed
     */
    private void markBatchAsFailed(List<Notification> batch, String errorMessage) {
        batch.forEach(notification -> {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(errorMessage);
        });
    }
    

    private void markAllAsFailed(List<Notification> notifications, String errorMessage) {
        notifications.forEach(notification -> {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(errorMessage);
        });
        notificationRepository.saveAll(notifications);
    }

    private <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }
}
