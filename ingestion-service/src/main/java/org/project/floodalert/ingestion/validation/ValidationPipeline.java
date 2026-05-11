package org.project.floodalert.ingestion.validation;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.validation.impl.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pipeline thực hiện validation theo thứ tự:
 * TimestampValidator
 * BlacklistValidator
 * IdentityStateValidator
 * SanityCheckValidator
 *
 */
@Slf4j
@Service
public class ValidationPipeline {
    
    private final List<SensorDataValidator> validators;
    
    public ValidationPipeline(
            TimestampValidator timestampValidator,
            BlacklistValidator blacklistValidator,
            IdentityStateValidator identityStateValidator,
            SanityCheckValidator sanityCheckValidator) {
        
        this.validators = List.of(
                timestampValidator,
                blacklistValidator,
                identityStateValidator,
                sanityCheckValidator
        );
        
        log.info("ValidationPipeline initialized with {} validators: {}", 
                validators.size(), 
                validators.stream().map(SensorDataValidator::getName).toList());
    }

    public boolean validate(ValidationContext context) {
        log.debug("Starting validation pipeline for sensor_id: {}", context.getSensorId());
        
        for (SensorDataValidator validator : validators) {
            boolean passed = validator.validate(context);
            
            if (!passed) {
                context.setValid(false);
                log.warn("Validation pipeline FAILED at step: {} for sensor_id: {}, reason: {}", 
                        context.getFailureStep(), 
                        context.getSensorId(), 
                        context.getFailureReason());
                return false;
            }
        }

        context.setValid(true);
        log.debug("Validation pipeline PASSED for sensor_id: {}", context.getSensorId());
        return true;
    }
}
