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

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic}")
    private String topic;

    @Override
    public void publish(SensorMessage sensorMessage) {
        kafkaTemplate.send(
                topic,
                sensorMessage.getSensorId(),
                sensorMessage.getRawPayload()
        );
    }
}
