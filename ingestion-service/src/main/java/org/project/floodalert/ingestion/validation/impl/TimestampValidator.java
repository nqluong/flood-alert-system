package org.project.floodalert.ingestion.validation.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.config.ValidationProperties;
import org.project.floodalert.ingestion.validation.SensorDataValidator;
import org.project.floodalert.ingestion.validation.ValidationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimestampValidator implements SensorDataValidator {
    
    private final ValidationProperties validationProperties;
    
    @Override
    public boolean validate(ValidationContext context) {
        Instant dataTimestamp = context.getSensorData().getTimestamp();
        
        if (dataTimestamp == null) {
            context.setFailureReason("Timestamp is null");
            context.setFailureStep(getName());
            log.warn("[{}] FAILED - sensor_id: {}, reason: timestamp is null", 
                    getName(), context.getSensorId());
            return false;
        }
        
        Instant now = context.getReceivedAt();
        long driftSeconds = Math.abs(Duration.between(dataTimestamp, now).getSeconds());
        
        if (driftSeconds > validationProperties.getMaxTimestampDriftSeconds()) {
            context.setFailureReason(String.format(
                    "Timestamp drift too large: %d seconds (max: %d seconds)", 
                    driftSeconds, validationProperties.getMaxTimestampDriftSeconds()));
            context.setFailureStep(getName());
            log.warn("[{}] FAILED - sensor_id: {}, drift: {}s, data_timestamp: {}, received_at: {}", 
                    getName(), context.getSensorId(), driftSeconds, dataTimestamp, now);
            return false;
        }
        
        log.debug("[{}] PASSED - sensor_id: {}, drift: {}s", 
                getName(), context.getSensorId(), driftSeconds);
        return true;
    }
    
    @Override
    public String getName() {
        return "TimestampValidator";
    }
}
