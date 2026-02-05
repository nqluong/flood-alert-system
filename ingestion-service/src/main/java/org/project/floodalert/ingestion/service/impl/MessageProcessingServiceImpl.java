package org.project.floodalert.ingestion.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.domain.SensorMessage;
import org.project.floodalert.ingestion.domain.ValidationResult;
import org.project.floodalert.ingestion.service.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessingServiceImpl implements MessageProcessingService {

    private final SensorIdExtractor sensorIdExtractor;
    private final MessageValidator messageValidator;
    private final MessagePublisher messagePublisher;
    private final SensorBlacklistService sensorBlacklistService;

    @Override
    public void process(String topic, String payload) {
        long startTime = System.currentTimeMillis();
        try{
            String sensorId = sensorIdExtractor.extract(topic);
            if(sensorId == null){
                log.warn("Could not extract sensor ID from topic: {}", topic);
                return;
            }

            if(sensorBlacklistService.isSensorBlacklisted(sensorId)){
                log.warn("Sensor is blacklisted - sensor_id: {}, topic: {}", sensorId, topic);
                return;
            }

            ValidationResult validation = messageValidator.validate(payload);

            if(!validation.isValid()){
                log.warn("Invalid payload - sensor_id: {}, reason: {}, payload_preview: {}",
                        sensorId, validation.getErrorMessage(),
                        payload.substring(0, Math.min(100, payload.length())));
                return;
            }

            SensorMessage message = SensorMessage.builder()
                    .sensorId(sensorId)
                    .rawPayload(payload)
                    .topic(topic)
                    .receivedAt(Instant.now())
                    .build();
            messagePublisher.publish(message);
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("Message processed in {} ms", processingTime);
        } catch (Exception e) {
            log.error("Unexpected error processing message - topic: {}, error: {}",
                    topic, e.getMessage(), e);
        }
    }
}
