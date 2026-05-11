package org.project.floodalert.ingestion.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.domain.SensorMessage;
import org.project.floodalert.ingestion.domain.ValidationResult;
import org.project.floodalert.ingestion.dto.SensorDataDTO;
import org.project.floodalert.ingestion.service.*;
import org.project.floodalert.ingestion.validation.ValidationContext;
import org.project.floodalert.ingestion.validation.ValidationPipeline;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessingServiceImpl implements MessageProcessingService {

    private final SensorIdExtractor sensorIdExtractor;
    private final MessageValidator messageValidator;
    private final MessagePublisher messagePublisher;
    private final SensorDataParser sensorDataParser;
    private final ValidationPipeline validationPipeline;
    private final CleanDTOBuilder cleanDTOBuilder;

    @Override
    public void process(String topic, String payload) {
        long startTime = System.currentTimeMillis();
        
        try {
            String sensorId = sensorIdExtractor.extract(topic);
            if (sensorId == null) {
                log.warn("Could not extract sensor ID from topic: {}", topic);
                return;
            }

            // Basic JSON validation
            ValidationResult validation = messageValidator.validate(payload);
            if (!validation.isValid()) {
                log.warn("Invalid JSON - sensor_id: {}, reason: {}, payload_preview: {}",
                        sensorId, validation.getErrorMessage(),
                        payload.substring(0, Math.min(100, payload.length())));
                return;
            }

            SensorDataDTO sensorData;
            try {
                sensorData = sensorDataParser.parse(payload);
            } catch (Exception e) {
                log.warn("Failed to parse sensor data - sensor_id: {}, error: {}, payload_preview: {}",
                        sensorId, e.getMessage(),
                        payload.substring(0, Math.min(100, payload.length())));
                return;
            }

            ValidationContext context = ValidationContext.builder()
                    .sensorId(sensorId)
                    .sensorData(sensorData)
                    .topic(topic)
                    .receivedAt(Instant.now())
                    .build();

            boolean isValid = validationPipeline.validate(context);
            
            if (!isValid) {
                log.warn("Validation FAILED - sensor_id: {}, step: {}, reason: {}",
                        sensorId, context.getFailureStep(), context.getFailureReason());
                return;
            }

            SensorMessage cleanMessage = cleanDTOBuilder.buildCleanMessage(context);
            
            messagePublisher.publish(cleanMessage);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("Message processed successfully in {} ms - sensor_id: {}",
                    processingTime, sensorId);
                    
        } catch (Exception e) {
            log.error("Unexpected error processing message - topic: {}, error: {}",
                    topic, e.getMessage(), e);
        }
    }
}
