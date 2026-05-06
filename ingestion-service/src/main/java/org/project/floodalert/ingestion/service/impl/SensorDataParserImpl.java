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
        // Parse JSON
        SensorDataDTO dto = objectMapper.readValue(jsonPayload, SensorDataDTO.class);
        
        // Validate using Bean Validation
        Set<ConstraintViolation<SensorDataDTO>> violations = validator.validate(dto);
        
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Validation failed: " + errors);
        }
        
        validateBusinessRules(dto);
        
        return dto;
    }
    

    private void validateBusinessRules(SensorDataDTO dto) {
        // Validate water level range
        Double waterLevel = dto.getTelemetry().getWaterLevel();
        if (waterLevel < 0) {
            throw new IllegalArgumentException(
                    "water_level out of range: " + waterLevel + " (must be >= 0)");
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
        
        if (dto.getHealth() != null && dto.getHealth().getBatteryLevel() != null) {
            double battery = dto.getHealth().getBatteryLevel();
            if (battery < 0 || battery > 100) {
                throw new IllegalArgumentException("Invalid battery level: " + battery);
            }
        }
    }
}
