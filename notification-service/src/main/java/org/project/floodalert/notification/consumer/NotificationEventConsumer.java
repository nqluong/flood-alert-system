package org.project.floodalert.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.event.FloodEventDTO;
import org.project.floodalert.notification.dto.event.FloodLifecycleEvent;
import org.project.floodalert.notification.enums.NotificationChannel;
import org.project.floodalert.notification.enums.NotificationPriority;
import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.project.floodalert.notification.model.NotificationContext;
import org.project.floodalert.notification.model.NotificationPreference;
import org.project.floodalert.notification.repository.NotificationPreferenceRepository;
import org.project.floodalert.notification.service.FcmDispatchService;
import org.project.floodalert.notification.service.NotificationAggregationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationAggregationService aggregationService;
    private final NotificationPreferenceRepository preferenceRepository;
    private final FcmDispatchService fcmDispatchService;

    private static final double DEFAULT_RADIUS_METERS = 5000.0; // 5km default
    private static final int MAX_RETRIES = 3;

    @KafkaListener(
            topics = "${app.kafka.topic.lifecycle-events}",
            groupId = "notification-push-group",
            containerFactory = "lifecycleKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeFloodLifecycleEvent(FloodLifecycleEvent event, Acknowledgment ack) {
        try {
            log.info("========== BẮT ĐẦU XỬ LÝ FLOOD LIFECYCLE EVENT ==========");
            log.info("Event ID: {}, Type: {}, Location: ({}, {}), Severity: {}",
                    event.getEventId(), event.getType(), event.getLat(), event.getLon(), 
                    event.getSeverityLevel());

            FloodEventDTO floodEventDTO = convertToFloodEventDTO(event);

            Map<UUID, NotificationContext> contexts = aggregationService.aggregateNotificationContexts(floodEventDTO);
            
            if (contexts.isEmpty()) {
                log.info("Không có user nào bị ảnh hưởng. Bỏ qua event.");
                ack.acknowledge();
                return;
            }
            
            log.info("Tìm thấy {} users bị ảnh hưởng", contexts.size());
            
            Map<UUID, NotificationContext> filteredContexts = filterThroughPreferences(contexts);
            
            if (filteredContexts.isEmpty()) {
                log.info("Không có user nào pass qua filter preferences. Bỏ qua event.");
                ack.acknowledge();
                return;
            }
            
            log.info("Sau khi filter: {} users sẽ nhận thông báo", filteredContexts.size());
            List<Notification> notifications = generateNotifications(filteredContexts, floodEventDTO);
            
            log.info("Đã tạo {} notifications", notifications.size());
            
            int successCount = fcmDispatchService.sendBatch(notifications);
            
            log.info("========== KẾT THÚC XỬ LÝ FLOOD LIFECYCLE EVENT ==========");
            log.info("Kết quả: {}/{} notifications gửi thành công", successCount, notifications.size());
            
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Lỗi khi xử lý event: eventId={}", event.getEventId(), e);
            // Không acknowledge để Kafka retry
        }
    }

    /**
     * Chuyển đổi FloodLifecycleEvent (từ FloodProcessor) sang FloodEventDTO (internal format)
     */
    private FloodEventDTO convertToFloodEventDTO(FloodLifecycleEvent event) {
        return FloodEventDTO.builder()
                .eventId(event.getEventId())
                .lat(event.getLat())
                .lon(event.getLon())
                .radiusMeters(DEFAULT_RADIUS_METERS) // Default 5km
                .severityLevel(event.getSeverityLevel())
                .waterLevel(event.getWaterLevel())
                .location(event.getLocation())
                .timestamp(event.getTimestamp())
                .build();
    }

    /**
     * Lọc users dựa trên preferences
     * Kiểm tra: enabled, floodAlerts, preferPush, quietHours
     */
    private Map<UUID, NotificationContext> filterThroughPreferences(
            Map<UUID, NotificationContext> contexts) {
        
        Set<UUID> userIds = contexts.keySet();
        
        // Fetch preferences một lần (batch query)
        List<NotificationPreference> preferences = preferenceRepository.findAllById(userIds);
        Map<UUID, NotificationPreference> preferenceMap = preferences.stream()
                .collect(Collectors.toMap(NotificationPreference::getUserId, p -> p));
        
        log.debug("Fetched {} preferences cho {} users", preferenceMap.size(), userIds.size());
        
        // Filter
        Map<UUID, NotificationContext> filtered = contexts.entrySet().stream()
                .filter(entry -> {
                    UUID userId = entry.getKey();
                    NotificationPreference pref = preferenceMap.get(userId);
                    
                    // Nếu không có preference, áp dụng default (cho phép)
                    if (pref == null) {
                        log.debug("User {} không có preference, áp dụng default (allow)", userId);
                        return true;
                    }
                    
                    // Check enabled
                    if (!Boolean.TRUE.equals(pref.getEnabled())) {
                        log.debug("User {} bị block: enabled=false", userId);
                        return false;
                    }
                    
                    // Check floodAlerts
                    if (!Boolean.TRUE.equals(pref.getFloodAlerts())) {
                        log.debug("User {} bị block: floodAlerts=false", userId);
                        return false;
                    }
                    
                    // Check preferPush
                    if (!Boolean.TRUE.equals(pref.getPreferPush())) {
                        log.debug("User {} bị block: preferPush=false", userId);
                        return false;
                    }
                    
                    // Check quiet hours
                    if (isInQuietHours(pref)) {
                        log.debug("User {} bị block: đang trong quiet hours", userId);
                        return false;
                    }
                    
                    return true;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        log.info("Filter hoàn tất: {}/{} users pass", filtered.size(), contexts.size());
        
        return filtered;
    }

    /**
     * Kiểm tra xem giờ hiện tại có nằm trong quiet hours không
     */
    private boolean isInQuietHours(NotificationPreference pref) {
        if (!Boolean.TRUE.equals(pref.getQuietHoursEnabled())) {
            return false;
        }

        LocalTime start = pref.getQuietHoursStart();
        LocalTime end = pref.getQuietHoursEnd();

        if (start == null || end == null) {
            return false;
        }

        LocalTime now = LocalTime.now();

        if (start.isBefore(end)) {
            // Normal case: 08:00 - 22:00
            return now.isAfter(start) && now.isBefore(end);
        } else {
            // Across midnight: 22:00 - 08:00
            return now.isAfter(start) || now.isBefore(end);
        }
    }

    /**
     * Sinh danh sách Notification từ contexts
     * Sử dụng context-based messaging (nội dung khác nhau tùy vào active/static)
     */
    private List<Notification> generateNotifications(
            Map<UUID, NotificationContext> contexts,
            FloodEventDTO event) {
        
        return contexts.values().stream()
                .map(context -> {
                    String title = aggregationService.generateNotificationTitle(context);
                    String body = aggregationService.generateNotificationBody(context, event);
                    Map<String, Object> data = buildNotificationData(event, context);
                    
                    return Notification.builder()
                            .userId(context.getUserId())
                            .title(title)
                            .body(body)
                            .notificationType("FLOOD_ALERT")
                            .priority(determinePriority(event.getSeverityLevel()))
                            .data(data)
                            .channel(NotificationChannel.PUSH)
                            .fcmToken(null) // Sẽ được điền bởi FcmDispatchService
                            .status(NotificationStatus.PENDING)
                            .retryCount(0)
                            .maxRetries(MAX_RETRIES)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Build data payload cho notification
     */
    private Map<String, Object> buildNotificationData(FloodEventDTO event, NotificationContext context) {
        Map<String, Object> data = new HashMap<>();
        
        // Event info
        data.put("eventId", event.getEventId());
        data.put("lat", event.getLat());
        data.put("lon", event.getLon());
        data.put("severityLevel", translateSeverityToVietnamese(event.getSeverityLevel()));
        data.put("waterLevel", event.getWaterLevel());
        data.put("location", event.getLocation());
        data.put("timestamp", event.getTimestamp().toString());
        
        data.put("isNearActive", context.isNearActive());
        // Chỉ lưu tên địa chỉ, không có UUID
        data.put("affectedZones", extractZoneNames(context.getAffectedZones()));
        
        if (context.getActiveDistance() != null) {
            data.put("activeDistance", context.getActiveDistance());
        }
        if (context.getStaticDistance() != null) {
            data.put("staticDistance", context.getStaticDistance());
        }
        
        return data;
    }
    
    /**
     * Trích xuất tên địa chỉ từ compound keys (loại bỏ UUID)
     * Input: ["uuid1::Hồng Mai", "uuid2::Đống Đa"]
     * Output: "Hồng Mai, Đống Đa"
     */
    private String extractZoneNames(List<String> affectedZones) {
        return affectedZones.stream()
                .map(zone -> {
                    // Nếu có dấu ::, lấy phần sau
                    int separatorIndex = zone.indexOf("::");
                    if (separatorIndex != -1 && separatorIndex < zone.length() - 2) {
                        return zone.substring(separatorIndex + 2);
                    }
                    // Nếu không có, trả về nguyên bản
                    return zone;
                })
                .collect(Collectors.joining(", "));
    }
    
    /**
     * Chuyển đổi mức độ ngập từ tiếng Anh sang tiếng Việt
     */
    private String translateSeverityToVietnamese(String severityLevel) {
        if (severityLevel == null) {
            return "Không xác định";
        }
        
        return switch (severityLevel.toUpperCase()) {
            case "CRITICAL" -> "Cực kỳ nguy hiểm";
            case "DANGER", "HIGH" -> "Nguy hiểm";
            case "WARNING", "MEDIUM" -> "Cảnh báo";
            case "LOW" -> "Thấp";
            default -> severityLevel; // Giữ nguyên nếu không match
        };
    }

    /**
     * Xác định priority dựa trên severity level
     */
    private NotificationPriority determinePriority(String severityLevel) {
        if (severityLevel == null) {
            return NotificationPriority.NORMAL;
        }

        return switch (severityLevel.toUpperCase()) {
            case "CRITICAL", "HIGH", "DANGER" -> NotificationPriority.HIGH;
            case "MEDIUM", "WARNING" -> NotificationPriority.NORMAL;
            default -> NotificationPriority.LOW;
        };
    }
}
