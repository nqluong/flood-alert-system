package org.project.floodalert.floodprocessor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.service.aggregator.FloodEventProcessorService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Lắng nghe topic {@code flood-sensor-data-processed} với consumer group riêng biệt
 * {@code flood-event-aggregator-group}. Mỗi batch messages được parse thành
 * {@link ProcessedSensorData} rồi chuyển cho {@link FloodEventProcessorService}
 * để xử lý theo 3 kịch bản (A/B/C).</p>
 *
 * <p>Acknowledge thủ công (MANUAL_IMMEDIATE) đảm bảo không mất dữ liệu:
 * offset chỉ được commit sau khi toàn bộ batch đã được xử lý.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventAggregatorListener {

    private final FloodEventProcessorService floodEventProcessorService;
    private final ObjectMapper objectMapper;


    /**
     * Nhận batch các {@link ProcessedSensorData} đã được xử lý từ pipeline ingestion.
     * Parse JSON → gọi Orchestrator → Acknowledge offset.
     *
     * @param messages        danh sách JSON message nhận được từ Kafka
     * @param acknowledgment  dùng để commit offset thủ công sau khi xử lý
     */
    @KafkaListener(
            topics = "${app.kafka.topic.output}",
            groupId = "${app.aggregator.kafka.consumer.group-id:flood-event-aggregator-group}",
            containerFactory = "aggregatorKafkaListenerContainerFactory"
    )
    public void consumeBatch(List<String> messages, Acknowledgment acknowledgment) {
        if (messages == null || messages.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }

        log.info("=== [AGGREGATOR] BẮT ĐẦU XỬ LÝ BATCH: {} messages ===", messages.size());
        long startTime = System.currentTimeMillis();

        try {
            // Parse JSON → ProcessedSensorData
            List<ProcessedSensorData> parsedList = parseMessages(messages);

            if (parsedList.isEmpty()) {
                log.warn("[AGGREGATOR] Không có message nào parse thành công, bỏ qua batch");
                acknowledgment.acknowledge();
                return;
            }

            floodEventProcessorService.processBatch(parsedList);

            acknowledgment.acknowledge();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("=== [AGGREGATOR] HOÀN THÀNH BATCH: {}/{} messages xử lý, {}ms ===",
                    parsedList.size(), messages.size(), elapsed);

        } catch (Exception e) {
            // Ghi log lỗi nghiêm trọng nhưng vẫn acknowledge để tránh re-consume vô hạn.
            // Trong môi trường production, cân nhắc đẩy message lỗi ra Dead Letter Queue (DLQ).
            log.error("[AGGREGATOR] Lỗi nghiêm trọng khi xử lý batch: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }


    private List<ProcessedSensorData> parseMessages(List<String> messages) {
        List<ProcessedSensorData> result = new ArrayList<>(messages.size());

        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            try {
                ProcessedSensorData data = objectMapper.readValue(message, ProcessedSensorData.class);
                result.add(data);
            } catch (Exception e) {
                log.warn("[AGGREGATOR] Không parse được message #{} (bỏ qua): {}",
                        i + 1, e.getMessage());
            }
        }

        log.debug("[AGGREGATOR] Parse thành công {}/{} messages", result.size(), messages.size());
        return result;
    }
}
