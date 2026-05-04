package org.project.floodalert.floodprocessor.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.service.aggregator.FloodEventProcessorService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class EventAggregatorListener {

    private final FloodEventProcessorService floodEventProcessorService;

    /**
     * Nhận batch các {@link ProcessedSensorData} đã được deserialize tự động từ Kafka.
     *
     * @param messages        danh sách ProcessedSensorData nhận được từ Kafka
     * @param acknowledgment  dùng để commit offset thủ công sau khi xử lý
     */
    @KafkaListener(
            topics = "${app.kafka.topic.output}",
            groupId = "${app.aggregator.kafka.consumer.group-id:flood-event-aggregator-group}",
            containerFactory = "aggregatorKafkaListenerContainerFactory"
    )
    public void consumeBatch(List<ProcessedSensorData> messages, Acknowledgment acknowledgment) {
        if (messages == null || messages.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }

        log.info("=== [AGGREGATOR] BẮT ĐẦU XỬ LÝ BATCH: {} messages ===", messages.size());
        long startTime = System.currentTimeMillis();

        try {
            floodEventProcessorService.processBatch(messages);

            acknowledgment.acknowledge();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("=== [AGGREGATOR] HOÀN THÀNH BATCH: {} messages xử lý, {}ms ===",
                    messages.size(), elapsed);

        } catch (Exception e) {
            log.error("[AGGREGATOR] Lỗi nghiêm trọng khi xử lý batch: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}
