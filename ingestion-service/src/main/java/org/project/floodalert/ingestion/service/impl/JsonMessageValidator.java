package org.project.floodalert.ingestion.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.domain.ValidationResult;
import org.project.floodalert.ingestion.service.MessageValidator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonMessageValidator implements MessageValidator {

    private final ObjectMapper objectMapper;

    @Override
    public ValidationResult validate(String payload) {

        if(payload == null || payload.trim().isEmpty()) {
            return ValidationResult.failure("Payload is null or empty");
        }
        try{
            JsonNode root = objectMapper.readTree(payload);
            if(!root.has("device_info")){
                return ValidationResult.failure("Missing device_info");
            }

            JsonNode device_info = root.get("device_info");
            if(!device_info.has("sensor_id")){
                return ValidationResult.failure("Missing device_info.sensor_id");
            }
            if(!root.has("telemetry")){
                return ValidationResult.failure("Missing telemetry");
            }

            if(!root.has("timestamp")){
                return ValidationResult.failure("Missing timestamp");
            }
            return ValidationResult.success();
        } catch (Exception e) {
            log.warn("Invalid JSON payload", e);
            return ValidationResult.failure("Invalid JSON: " + e.getMessage());
        }

    }
}
