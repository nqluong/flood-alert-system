package org.project.floodalert.notification.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.event.FloodLifecycleEvent;
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
            // Đẩy thẳng sự kiện cảnh báo xuống client đang subscribe /topic/admin/alerts
            simpMessagingTemplate.convertAndSend(TOPIC_ALERTS, event);

            log.debug("[ALERT] Đẩy WebSocket cảnh báo thành công eventId={}, type={}, location={}, severity={}",
                    event.getEventId(), event.getType(), event.getLocation(), event.getSeverityLevel());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            // Ghi log lỗi nhưng vẫn acknowledge để tránh block consumer vô hạn
            log.error("[ALERT] Lỗi xử lý lifecycle event, bỏ qua: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}
