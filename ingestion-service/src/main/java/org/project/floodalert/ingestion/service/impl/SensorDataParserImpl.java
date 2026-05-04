package org.project.floodalert.ingestion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.dto.SensorDataDTO;
import org.project.floodalert.ingestion.service.SensorDataParser;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataParserImpl implements SensorDataParser {
    
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Override
    public SensorDataDTO parse(String jsonPayload) throws Exception {
        // Step 1: Parse JSON
        SensorDataDTO dto = objectMapper.readValue(jsonPayload, SensorDataDTO.class);
        
        // Step 2: Validate using Bean Validation
        Set<ConstraintViolation<SensorDataDTO>> violations = validator.validate(dto);
        
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Validation failed: " + errors);
        }
        
        // Step 3: Business validation
        validateBusinessRules(dto);
        
        return dto;
    }
    
    /**
     * Validate business rules
     */
    private void validateBusinessRules(SensorDataDTO dto) {
        // Validate water level range
        Double waterLevel = dto.getTelemetry().getWaterLevel();
        if (waterLevel < 0 || waterLevel > 10) {
            throw new IllegalArgumentException(
                    "water_level out of range: " + waterLevel + " (expected: 0-10m)");
        }
        
        // Validate coordinates if present
        if (dto.getTelemetry().getLat() != null) {
            double lat = dto.getTelemetry().getLat();
            if (lat < -90 || lat > 90) {
                throw new IllegalArgumentException("Invalid latitude: " + lat);
            }
        }
        
        if (dto.getTelemetry().getLon() != null) {
            double lon = dto.getTelemetry().getLon();
            if (lon < -180 || lon > 180) {
                throw new IllegalArgumentException("Invalid longitude: " + lon);
            }
        }
        
        // Validate battery level if present
        if (dto.getHealth() != null && dto.getHealth().getBatteryLevel() != null) {
            double battery = dto.getHealth().getBatteryLevel();
            if (battery < 0 || battery > 100) {
                throw new IllegalArgumentException("Invalid battery level: " + battery);
            }
        }
    }
}
