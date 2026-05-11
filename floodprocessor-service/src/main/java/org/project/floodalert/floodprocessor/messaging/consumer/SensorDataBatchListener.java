package org.project.floodalert.floodprocessor.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.SensorMessage;
import org.project.floodalert.floodprocessor.service.processing.SensorDataBatchProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class SensorDataBatchListener {
    
    private final SensorDataBatchProcessor sensorDataBatchProcessor;

    @KafkaListener(
            topics = "${app.kafka.topic.ingest}",
            groupId = "${app.kafka.consumer.group-id}",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeBatch(List<SensorMessage> messages, Acknowledgment acknowledgment) {
        log.debug("Nhận batch {} sensor messages từ Kafka", messages.size());
        
        try {
            sensorDataBatchProcessor.processBatch(messages);
            acknowledgment.acknowledge();
            log.debug("Xử lý và acknowledge batch {} messages thành công", messages.size());
            
        } catch (Exception e) {
            log.error("Lỗi khi xử lý batch {} messages: {}", 
                    messages.size(), e.getMessage(), e);
            // Acknowledge để tránh retry vô hạn với bad messages
            acknowledgment.acknowledge();
        }
    }
}
