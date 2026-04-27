package org.project.floodalert.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.UserGeoDTO;
import org.project.floodalert.notification.dto.event.FloodLifecycleEvent;
import org.project.floodalert.notification.enums.NotificationChannel;
import org.project.floodalert.notification.enums.NotificationPriority;
import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.project.floodalert.notification.model.NotificationPreference;
import org.project.floodalert.notification.repository.NotificationPreferenceRepository;
import org.project.floodalert.notification.repository.NotificationRepository;
import org.project.floodalert.notification.service.RedisGeoService;
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

    private final RedisGeoService redisGeoService;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationRepository notificationRepository;

    private static final double RADAR_RADIUS_KM = 5.0;
    private static final int MAX_RETRIES = 3;

    @KafkaListener(
            topics = "${app.kafka.topic.lifecycle-events}",
            groupId = "notification-push-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeFloodLifecycleEvent(FloodLifecycleEvent event, Acknowledgment ack) {
        try {
            log.info("Nhận event từ Kafka: eventId={}, type={}, location=({}, {})",
                    event.getEventId(), event.getType(), event.getLat(), event.getLon());

            // B1: Quét tìm users trong bán kính 5km
            List<UserGeoDTO> nearbyUsers = scanRadar(event.getLat(), event.getLon());
            log.info("Quét Radar hoàn tất: Tìm thấy {} users trong bán kính {}km",
                    nearbyUsers.size(), RADAR_RADIUS_KM);

            if (nearbyUsers.isEmpty()) {
                log.info("Không có user nào trong vùng ảnh hưởng. Bỏ qua event.");
                ack.acknowledge();
                return;
            }

            List<UserGeoDTO> filteredUsers = filterThroughFunnel(nearbyUsers);
            log.info("Lọc qua Phễu hoàn tất: {} users pass qua filter (từ {} users ban đầu)",
                    filteredUsers.size(), nearbyUsers.size());

            if (filteredUsers.isEmpty()) {
                log.info("Không có user nào pass qua filter. Bỏ qua event.");
                ack.acknowledge();
                return;
            }

            List<Notification> pendingNotifications = createPendingNotifications(filteredUsers, event);
            notificationRepository.saveAll(pendingNotifications);

            log.info("Đã lưu {} notifications vào Outbox với status=PENDING", pendingNotifications.size());

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Lỗi khi xử lý event: eventId={}", event.getEventId(), e);
            // Không acknowledge để Kafka retry
        }
    }

    private List<UserGeoDTO> scanRadar(Double lat, Double lon) {
        try {
            return redisGeoService.findUsersNear(lat, lon, RADAR_RADIUS_KM);
        } catch (Exception e) {
            log.error(" Lỗi khi quét radar tại location=({}, {})", lat, lon, e);
            return Collections.emptyList();
        }
    }

    private List<UserGeoDTO> filterThroughFunnel(List<UserGeoDTO> nearbyUsers) {
        // Lấy tất cả userIds
        Set<UUID> userIds = nearbyUsers.stream()
                .map(UserGeoDTO::getUserId)
                .collect(Collectors.toSet());

        // Truy vấn preferences một lần
        List<NotificationPreference> preferences = preferenceRepository.findAllById(userIds);
        Map<UUID, NotificationPreference> preferenceMap = preferences.stream()
                .collect(Collectors.toMap(NotificationPreference::getUserId, p -> p));

        // Lọc users theo các tiêu chí
        return nearbyUsers.stream()
                .filter(userGeo -> {
                    NotificationPreference pref = preferenceMap.get(userGeo.getUserId());

                    // Nếu không có preference, tạo mặc định (enabled=true, floodAlerts=true, preferPush=true)
                    if (pref == null) {
                        log.debug("User {} không có preference, áp dụng default filter", userGeo.getUserId());
                        return userGeo.getDistance() <= 500; // Default 500m
                    }

                    // enabled == true
                    if (!Boolean.TRUE.equals(pref.getEnabled())) {
                        log.debug("User {} bị block: enabled=false", userGeo.getUserId());
                        return false;
                    }

                    // floodAlerts == true
                    if (!Boolean.TRUE.equals(pref.getFloodAlerts())) {
                        log.debug("User {} bị block: floodAlerts=false", userGeo.getUserId());
                        return false;
                    }

                    // preferPush == true
                    if (!Boolean.TRUE.equals(pref.getPreferPush())) {
                        log.debug("User {} bị block: preferPush=false", userGeo.getUserId());
                        return false;
                    }

                    // distance <= alertRadiusMeters
                    int alertRadius = pref.getAlertRadiusMeters() != null ? pref.getAlertRadiusMeters() : 500;
                    if (userGeo.getDistance() > alertRadius) {
                        log.debug("User {} bị block: distance={}m > alertRadius={}m",
                                userGeo.getUserId(), userGeo.getDistance(), alertRadius);
                        return false;
                    }

                    // Không trong quiet hours
                    if (isInQuietHours(pref)) {
                        log.debug("User {} bị block: đang trong quiet hours", userGeo.getUserId());
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
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
            // Normal case: start=22:00, end=08:00 -> quiet from 22:00 to 08:00
            return now.isAfter(start) && now.isBefore(end);
        } else {
            // Across midnight: start=22:00, end=08:00 -> quiet from 22:00 to 23:59 and 00:00 to 08:00
            return now.isAfter(start) || now.isBefore(end);
        }
    }


    private List<Notification> createPendingNotifications(List<UserGeoDTO> filteredUsers,
                                                           FloodLifecycleEvent event) {
        return filteredUsers.stream()
                .map(userGeo -> {
                    String title = buildNotificationTitle(event);
                    String body = buildNotificationBody(event, userGeo.getDistance());
                    Map<String, Object> data = buildNotificationData(event);

                    return Notification.builder()
                            .userId(userGeo.getUserId())
                            .title(title)
                            .body(body)
                            .notificationType("FLOOD_ALERT")
                            .priority(determinePriority(event.getSeverityLevel()))
                            .data(data)
                            .channel(NotificationChannel.PUSH)
                            .fcmToken(null) // Sẽ được điền bởi FcmDispatchWorker
                            .status(NotificationStatus.PENDING)
                            .retryCount(0)
                            .maxRetries(MAX_RETRIES)
                            .nextRetryAt(null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String buildNotificationTitle(FloodLifecycleEvent event) {
        return "Cảnh báo ngập lụt";
    }

    private String buildNotificationBody(FloodLifecycleEvent event, Double distance) {
        return String.format("Phát hiện ngập lụt gần vị trí của bạn (cách %.0fm). Mức độ: %s. Hãy cẩn thận!",
                distance, event.getSeverityLevel());
    }

    private Map<String, Object> buildNotificationData(FloodLifecycleEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getEventId());
        data.put("type", event.getType());
        data.put("waterLevel", event.getWaterLevel());
        data.put("severityLevel", event.getSeverityLevel());
        data.put("location", event.getLocation());
        data.put("lat", event.getLat());
        data.put("lon", event.getLon());
        data.put("timestamp", event.getTimestamp().toString());
        return data;
    }

    private NotificationPriority determinePriority(String severityLevel) {
        if (severityLevel == null) {
            return NotificationPriority.NORMAL;
        }

        return switch (severityLevel.toUpperCase()) {
            case "CRITICAL", "HIGH" -> NotificationPriority.HIGH;
            case "MEDIUM" -> NotificationPriority.NORMAL;
            default -> NotificationPriority.LOW;
        };
    }
}
