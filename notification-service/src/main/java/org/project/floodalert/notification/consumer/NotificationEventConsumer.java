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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationAggregationService aggregationService;
    private final NotificationPreferenceRepository preferenceRepository;
    private final FcmDispatchService fcmDispatchService;

    private static final double DEFAULT_RADIUS_METERS = 5000.0;
    private static final int MAX_RETRIES = 3;

    private static final String TYPE_RESOLVED = "RESOLVED";
    private static final String NOTIFICATION_TYPE_FLOOD_ALERT = "FLOOD_ALERT";
    private static final String NOTIFICATION_TYPE_FLOOD_RESOLVED = "FLOOD_RESOLVED";

    @KafkaListener(
            topics = "${app.kafka.topic.lifecycle-events}",
            groupId = "notification-push-group",
            containerFactory = "lifecycleKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeFloodLifecycleEvent(FloodLifecycleEvent event, Acknowledgment ack) {
        try {
            boolean isResolved = TYPE_RESOLVED.equalsIgnoreCase(event.getType());
            log.info("[NotifConsumer] Xử lý event eventId={}, type={}, severity={}, location=({},{})",
                    event.getEventId(), event.getType(), event.getSeverityLevel(),
                    event.getLat(), event.getLon());

            FloodEventDTO dto = convertToFloodEventDTO(event);

            Map<UUID, NotificationContext> contexts =
                    aggregationService.aggregateNotificationContexts(dto, !isResolved);

            if (contexts.isEmpty()) {
                log.info("[NotifConsumer] Không có user bị ảnh hưởng, bỏ qua eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            Map<UUID, NotificationContext> filtered = filterThroughPreferences(contexts);
            if (filtered.isEmpty()) {
                log.info("[NotifConsumer] Không có user nào pass filter preferences, eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            List<Notification> notifications = generateNotifications(filtered, dto, isResolved);
            int successCount = fcmDispatchService.sendBatch(notifications);

            log.info("[NotifConsumer] Hoàn tất eventId={}: {}/{} notifications gửi thành công",
                    event.getEventId(), successCount, notifications.size());
            ack.acknowledge();

        } catch (Exception e) {
            log.error("[NotifConsumer] Lỗi khi xử lý event: eventId={}", event.getEventId(), e);
        }
    }

    private FloodEventDTO convertToFloodEventDTO(FloodLifecycleEvent event) {
        return FloodEventDTO.builder()
                .eventId(event.getEventId())
                .lat(event.getLat())
                .lon(event.getLon())
                .radiusMeters(DEFAULT_RADIUS_METERS)
                .severityLevel(event.getSeverityLevel())
                .waterLevel(event.getWaterLevel())
                .location(event.getLocation())
                .timestamp(event.getTimestamp())
                .build();
    }

    /**
     * Filter users theo preferences: enabled, floodAlerts, preferPush, quietHours.
     */
    private Map<UUID, NotificationContext> filterThroughPreferences(Map<UUID, NotificationContext> contexts) {
        Set<UUID> userIds = contexts.keySet();
        Map<UUID, NotificationPreference> preferenceMap = preferenceRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(NotificationPreference::getUserId, p -> p));

        return contexts.entrySet().stream()
                .filter(entry -> isAllowed(preferenceMap.get(entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isAllowed(NotificationPreference pref) {
        if (pref == null) return true;

        if (!Boolean.TRUE.equals(pref.getEnabled())
                || !Boolean.TRUE.equals(pref.getFloodAlerts())
                || !Boolean.TRUE.equals(pref.getPreferPush())) {
            return false;
        }
        return !isInQuietHours(pref);
    }

    private boolean isInQuietHours(NotificationPreference pref) {
        if (!Boolean.TRUE.equals(pref.getQuietHoursEnabled())) return false;

        LocalTime start = pref.getQuietHoursStart();
        LocalTime end = pref.getQuietHoursEnd();
        if (start == null || end == null) return false;

        LocalTime now = LocalTime.now();
        return start.isBefore(end)
                ? (now.isAfter(start) && now.isBefore(end))
                : (now.isAfter(start) || now.isBefore(end));
    }

    private List<Notification> generateNotifications(Map<UUID, NotificationContext> contexts,
                                                     FloodEventDTO event,
                                                     boolean isResolved) {
        return contexts.values().stream()
                .map(context -> buildNotification(context, event, isResolved))
                .collect(Collectors.toList());
    }

    private Notification buildNotification(NotificationContext context, FloodEventDTO event, boolean isResolved) {
        String title = isResolved
                ? aggregationService.generateResolvedTitle()
                : aggregationService.generateNotificationTitle(context);
        String body = isResolved
                ? aggregationService.generateResolvedBody(context, event)
                : aggregationService.generateNotificationBody(context, event);

        return Notification.builder()
                .userId(context.getUserId())
                .title(title)
                .body(body)
                .notificationType(isResolved ? NOTIFICATION_TYPE_FLOOD_RESOLVED : NOTIFICATION_TYPE_FLOOD_ALERT)
                .priority(isResolved ? NotificationPriority.NORMAL : determinePriority(event.getSeverityLevel()))
                .data(buildNotificationData(event, context, isResolved))
                .channel(NotificationChannel.PUSH)
                .fcmToken(null)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .maxRetries(MAX_RETRIES)
                .build();
    }

    private Map<String, Object> buildNotificationData(FloodEventDTO event, NotificationContext context, boolean isResolved) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getEventId());
        data.put("lat", event.getLat());
        data.put("lon", event.getLon());
        data.put("severityLevel", translateSeverityToVietnamese(event.getSeverityLevel()));
        data.put("waterLevel", event.getWaterLevel());
        data.put("location", event.getLocation());
        data.put("timestamp", event.getTimestamp().toString());
        data.put("isResolved", isResolved);
        data.put("isNearActive", context.isNearActive());
        data.put("affectedZones", String.join(", ", context.getAffectedZones()));

        if (context.getActiveDistance() != null) data.put("activeDistance", context.getActiveDistance());
        if (context.getStaticDistance() != null) data.put("staticDistance", context.getStaticDistance());

        return data;
    }

    private String translateSeverityToVietnamese(String severityLevel) {
        if (severityLevel == null) return "Không xác định";
        return switch (severityLevel.toUpperCase()) {
            case "CRITICAL" -> "Cực kỳ nguy hiểm";
            case "DANGER", "HIGH" -> "Nguy hiểm";
            case "WARNING", "MEDIUM" -> "Cảnh báo";
            case "LOW" -> "Thấp";
            default -> severityLevel;
        };
    }

    private NotificationPriority determinePriority(String severityLevel) {
        if (severityLevel == null) return NotificationPriority.NORMAL;
        return switch (severityLevel.toUpperCase()) {
            case "CRITICAL", "HIGH", "DANGER" -> NotificationPriority.HIGH;
            case "MEDIUM", "WARNING" -> NotificationPriority.NORMAL;
            default -> NotificationPriority.LOW;
        };
    }
}
