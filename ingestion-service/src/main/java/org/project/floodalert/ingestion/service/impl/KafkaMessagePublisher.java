package org.project.floodalert.ingestion.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.domain.SensorMessage;
import org.project.floodalert.ingestion.service.MessagePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessagePublisher implements MessagePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic}")
    private String topic;

    @Override
    public void publish(SensorMessage sensorMessage) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                topic,
                sensorMessage.getSensorId(),
                sensorMessage
        );

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[INGESTION] Gửi Kafka thất bại cho sensor {}: {}",
                        sensorMessage.getSensorId(), ex.getMessage());
            }
        });
    }
}
