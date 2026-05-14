package org.project.floodalert.notification.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.event.FloodLifecycleEvent;
import org.project.floodalert.notification.dto.response.AdminAlertDTO;
import org.project.floodalert.notification.dto.response.ProcessedSensorData;
import org.project.floodalert.notification.dto.response.SensorTelemetryWsDTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSocketDispatcher {

    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final String TOPIC_TELEMETRY = "/topic/admin/map/telemetry";

    private static final String TOPIC_ALERTS = "/topic/admin/alerts";

    @KafkaListener(
            topics = "${app.kafka.topic.processed-events}",
            groupId = "${app.kafka.consumer.telemetry.group-id:admin-socket-telemetry-group}",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void onTelemetryEvent(ProcessedSensorData data, Acknowledgment acknowledgment) {
        try {
            SensorTelemetryWsDTO slimDto = SensorTelemetryWsDTO.builder()
                    .sensorId(data.getSensorId())
                    .waterLevel(data.getWaterLevel())
                    .lat(data.getLat())
                    .lon(data.getLon())
                    .battery(data.getBattery())
                    .warningThreshold(data.getWarningThreshold())
                    .dangerThreshold(data.getDangerThreshold())
                    .timestamp(data.getTimestamp())
                    .locationName(data.getLocationName())
                    .build();
            simpMessagingTemplate.convertAndSend(TOPIC_TELEMETRY, slimDto);


            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("[TELEMETRY] Lỗi xử lý message telemetry, bỏ qua: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topic.lifecycle-events}",
            groupId = "${app.kafka.consumer.alert.group-id:admin-socket-alert-group}",
            containerFactory = "lifecycleKafkaListenerContainerFactory"
    )
    public void onLifecycleAlert(FloodLifecycleEvent event, Acknowledgment acknowledgment) {
        try {
            AdminAlertDTO alertDTO = enrichEventForAdmin(event);
            
            simpMessagingTemplate.convertAndSend(TOPIC_ALERTS, alertDTO);

            log.debug("[ALERT] Đẩy WebSocket cảnh báo thành công eventId={}, type={}, location={}, severity={}",
                    event.getEventId(), event.getType(), event.getLocation(), event.getSeverityLevel());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            // Ghi log lỗi nhưng vẫn acknowledge để tránh block consumer vô hạn
            log.error("[ALERT] Lỗi xử lý lifecycle event, bỏ qua: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Enrich event với thông tin dễ hiểu cho admin dashboard
     */
    private AdminAlertDTO enrichEventForAdmin(FloodLifecycleEvent event) {
        String eventType = event.getType();
        String location = resolveLocation(event);
        String alertMessage = generateAlertMessage(eventType, event, location);
        String alertLevel = determineAlertLevel(eventType, event.getSeverityLevel());

        return AdminAlertDTO.builder()
                .eventId(event.getEventId())
                .type(eventType)
                .waterLevel(event.getWaterLevel())
                .severityLevel(event.getSeverityLevel())
                .location(location)
                .lat(event.getLat())
                .lon(event.getLon())
                .timestamp(event.getTimestamp())
                .alertMessage(alertMessage)
                .alertLevel(alertLevel)
                .build();
    }

    private String resolveLocation(FloodLifecycleEvent event) {
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            return event.getLocation();
        }
        // Fallback: lấy sensorId từ eventId (format: EV-yyyyMMdd-HHmmss-{sensorId})
        String eventId = event.getEventId();
        if (eventId != null && eventId.startsWith("EV-") && eventId.length() > 19) {
            String sensorId = eventId.substring(19);
            return "Sensor " + sensorId;
        }
        if (event.getLat() != null && event.getLon() != null) {
            return String.format("Vị trí [%.4f, %.4f]", event.getLat(), event.getLon());
        }
        return "Không xác định";
    }

    private String generateAlertMessage(String eventType, FloodLifecycleEvent event, String location) {
        if (eventType == null) {
            return String.format("Cập nhật tình trạng ngập tại %s", location);
        }

        return switch (eventType.toUpperCase()) {
            case "CREATED" ->
                String.format("Phát hiện ngập mới tại %s - Mức độ: %s",
                    location, translateSeverity(event.getSeverityLevel()));

            case "ESCALATED" ->
                String.format("Mức độ ngập gia tăng tại %s - Hiện tại: %s",
                    location, translateSeverity(event.getSeverityLevel()));

            case "DE_ESCALATED" ->
                String.format("Mức độ ngập giảm tại %s - Hiện tại: %s",
                    location, translateSeverity(event.getSeverityLevel()));

            case "RESOLVED" ->
                String.format("Tình trạng ngập đã được giải quyết tại %s", location);

            case "UPDATED" ->
                String.format("Cập nhật tình trạng ngập tại %s - Mức độ: %s",
                    location, translateSeverity(event.getSeverityLevel()));

            default ->
                String.format("Cập nhật tình trạng ngập tại %s", location);
        };
    }

    private String determineAlertLevel(String eventType, String severityLevel) {
        if (eventType == null) {
            return "INFO";
        }
        
        return switch (eventType.toUpperCase()) {
            case "CREATED" -> {
                if ("CRITICAL".equalsIgnoreCase(severityLevel) || "HIGH".equalsIgnoreCase(severityLevel) 
                        || "DANGER".equalsIgnoreCase(severityLevel)) {
                    yield "CRITICAL";
                }
                yield "WARNING";
            }
            case "ESCALATED" -> "DANGER";
            case "DE_ESCALATED" -> "SUCCESS";
            case "RESOLVED" -> "SUCCESS";
            case "UPDATED" -> "INFO";
            default -> "INFO";
        };
    }

    private String translateSeverity(String severityLevel) {
        if (severityLevel == null) {
            return "Không xác định";
        }
        
        return switch (severityLevel.toUpperCase()) {
            case "CRITICAL" -> "Cực kỳ nguy hiểm";
            case "DANGER", "HIGH" -> "Nguy hiểm";
            case "WARNING", "MEDIUM" -> "Cảnh báo";
            case "LOW" -> "Thấp";
            default -> severityLevel;
        };
    }
}
