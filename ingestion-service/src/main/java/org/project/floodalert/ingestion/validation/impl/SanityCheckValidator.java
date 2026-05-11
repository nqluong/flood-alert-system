package org.project.floodalert.ingestion.validation.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.config.ValidationProperties;
import org.project.floodalert.ingestion.validation.SensorDataValidator;
import org.project.floodalert.ingestion.validation.ValidationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SanityCheckValidator implements SensorDataValidator {
    
    private final ValidationProperties validationProperties;
    
    @Override
    public boolean validate(ValidationContext context) {
        Double waterLevel = context.getSensorData().getTelemetry().getWaterLevel();
        
        if (waterLevel == null) {
            context.setFailureReason("Water level is null");
            context.setFailureStep(getName());
            log.warn("[{}] FAILED - sensor_id: {}, water_level is null", 
                    getName(), context.getSensorId());
            return false;
        }
        
        double minLevel = validationProperties.getMinWaterLevel();
        double maxLevel = validationProperties.getMaxWaterLevel();
        
        if (waterLevel < minLevel || waterLevel > maxLevel) {
            context.setFailureReason(String.format(
                    "Water level out of range: %.2f cm (valid range: %.2f - %.2f cm)", 
                    waterLevel, minLevel, maxLevel));
            context.setFailureStep(getName());
            log.warn("[{}] FAILED - sensor_id: {}, water_level: {} cm (out of range)", 
                    getName(), context.getSensorId(), waterLevel);
            return false;
        }
        
        log.debug("[{}] PASSED - sensor_id: {}, water_level: {} cm", 
                getName(), context.getSensorId(), waterLevel);
        return true;
    }
    
    @Override
    public String getName() {
        return "SanityCheckValidator";
    }
}
