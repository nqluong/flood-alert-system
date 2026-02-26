package org.project.floodalert.floodcore.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.dto.event.SensorHealthSyncEvent;
import org.project.floodalert.floodcore.repository.SensorBatchUpdateRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class SensorHealthBatchListener {

    private final SensorBatchUpdateRepository sensorBatchUpdateRepository;

    /**
     * Lắng nghe batch events từ topic sensor-health-sync.
     *
     * @param events         lô {@link SensorHealthSyncEvent} nhận được từ Kafka
     * @param acknowledgment dùng để commit offset thủ công sau khi xử lý
     */
    @KafkaListener(
            topics = "${app.kafka.topic.sensor-health-sync:sensor-health-sync}",
            groupId = "${app.kafka.consumer.sensor-health.group-id:sensor-health-batch-group}",
            containerFactory = "sensorHealthBatchListenerContainerFactory"
    )
    public void consumeBatch(List<SensorHealthSyncEvent> events, Acknowledgment acknowledgment) {
        if (events == null || events.isEmpty()) {
            log.warn("[SensorHealthListener] Nhận batch rỗng, bỏ qua xử lý.");
            acknowledgment.acknowledge();
            return;
        }

        log.info("[SensorHealthListener] === BẮT ĐẦU XỬ LÝ BATCH: {} sensor health events ===",
                events.size());
        long startTime = System.currentTimeMillis();

        try {
            sensorBatchUpdateRepository.batchUpdateSensorHealth(events);

            acknowledgment.acknowledge();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[SensorHealthListener] === HOÀN THÀNH BATCH: {} events, tổng thời gian: {}ms ===",
                    events.size(), elapsed);

        } catch (Exception e) {
            log.error("[SensorHealthListener] Lỗi nghiêm trọng khi batch update sensor health (size={}): {}",
                    events.size(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}
