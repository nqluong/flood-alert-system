package org.project.floodalert.floodprocessor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.event.SensorHealthSyncEvent;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
@RequiredArgsConstructor
public class SensorHealthThrottlingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.sensor-health-sync:sensor-health-sync}")
    private String sensorHealthSyncTopic;

    @Value("${app.sensor-health.sync-interval-ms:300000}")
    private long syncIntervalMs;

    /** Cache in-memory: sensorId thời điểm đồng bộ cuối (Unix ms) */
    private final ConcurrentHashMap<String, Long> lastSyncTimeMap = new ConcurrentHashMap<>();

    /** Cache in-memory: sensorId trạng thái lần đồng bộ cuối */
    private final ConcurrentHashMap<String, String> lastStatusMap = new ConcurrentHashMap<>();


    public void checkAndSend(ProcessedSensorData data) {
        String sensorId = data.getSensorId();
        String currentStatus = resolveStatus(data);
        long now = System.currentTimeMillis();

        String cachedStatus = lastStatusMap.get(sensorId);
        Long cachedSyncTime = lastSyncTimeMap.get(sensorId);

        boolean statusChanged = !currentStatus.equals(cachedStatus);
        boolean intervalExpired = cachedSyncTime == null || (now - cachedSyncTime) > syncIntervalMs;

        if (!statusChanged && !intervalExpired) {
            long remaining = syncIntervalMs - (now - cachedSyncTime); // safe: intervalExpired=false ⟹ cachedSyncTime != null
            log.debug("[HealthThrottling] Bỏ qua sensor={}: status không đổi ({}) và chưa đến chu kỳ đồng bộ ({}ms còn lại)",
                    sensorId, currentStatus, remaining);
            return;
        }

        if (statusChanged) {
            log.info("[HealthThrottling] Gửi health event sensor={}: status thay đổi [{} → {}]",
                    sensorId, cachedStatus, currentStatus);
        } else {
            long elapsedSinceLastSync = cachedSyncTime != null ? now - cachedSyncTime : syncIntervalMs;
            log.info("[HealthThrottling] Gửi health event sensor={}: chu kỳ đồng bộ định kỳ ({}ms đã trôi qua)",
                    sensorId, elapsedSinceLastSync);
        }

        SensorHealthSyncEvent event = buildEvent(data, currentStatus, now);
        sendToKafka(sensorId, event);

        // Cập nhật cache sau khi gửi thành công
        lastSyncTimeMap.put(sensorId, now);
        lastStatusMap.put(sensorId, currentStatus);
    }

    private String resolveStatus(ProcessedSensorData data) {
        if (data.getDeviceStatus() == null) {
            return "UNKNOWN";
        }
        return data.getDeviceStatus();
    }

    private SensorHealthSyncEvent buildEvent(ProcessedSensorData data, String status, long now) {
        return SensorHealthSyncEvent.builder()
                .sensorId(data.getSensorId())
                .battery(data.getBattery())
                .signalStrength(data.getSignalStrength())
                .status(status)
                .timestamp(now)
                .build();
    }

    private void sendToKafka(String sensorId, SensorHealthSyncEvent event) {
        kafkaTemplate.send(sensorHealthSyncTopic, sensorId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[HealthThrottling] Gửi Kafka thất bại cho sensor={}: {}",
                                sensorId, ex.getMessage(), ex);
                    } else {
                        log.debug("[HealthThrottling] Kafka ACK sensor={}, topic={}, partition={}, offset={}",
                                sensorId,
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
