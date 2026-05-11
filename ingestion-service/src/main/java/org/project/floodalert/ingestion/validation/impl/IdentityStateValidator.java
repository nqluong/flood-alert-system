package org.project.floodalert.ingestion.validation.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.config.ValidationProperties;
import org.project.floodalert.ingestion.validation.SensorDataValidator;
import org.project.floodalert.ingestion.validation.ValidationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityStateValidator implements SensorDataValidator {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValidationProperties validationProperties;
    
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "TESTING");
    
    @Override
    public boolean validate(ValidationContext context) {
        String sensorId = context.getSensorId();
        String sensorInfoKey = validationProperties.getSensorInfoKeyPrefix() + sensorId;
        
        try {
            Map<Object, Object> sensorInfo = redisTemplate.opsForHash().entries(sensorInfoKey);
            
            if (sensorInfo.isEmpty()) {
                context.setFailureReason("Sensor not registered in Redis");
                context.setFailureStep(getName());
                log.warn("[{}] FAILED - sensor_id: {} not found in Redis", 
                        getName(), sensorId);
                return false;
            }
            
            Map<String, String> stringMap = sensorInfo.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> String.valueOf(e.getKey()),
                            e -> String.valueOf(e.getValue())
                    ));
            context.setSensorInfo(stringMap);
            
            // Kiểm tra API Key
            String payloadApiKey = context.getSensorData().getDeviceInfo().getApiKey();
            String redisApiKey = (String) sensorInfo.get("api_key");
            
            if (payloadApiKey == null || !payloadApiKey.equals(redisApiKey)) {
                context.setFailureReason("API key mismatch");
                context.setFailureStep(getName());
                log.warn("[{}] FAILED - sensor_id: {}, api_key mismatch", 
                        getName(), sensorId);
                return false;
            }
            
            // Kiểm tra Status
            String status = (String) sensorInfo.get("status");
            
            if (status == null || !ALLOWED_STATUSES.contains(status.toUpperCase())) {
                context.setFailureReason(String.format(
                        "Invalid status: %s (allowed: %s)", status, ALLOWED_STATUSES));
                context.setFailureStep(getName());
                log.warn("[{}] FAILED - sensor_id: {}, status: {}", 
                        getName(), sensorId, status);
                return false;
            }
            
            log.debug("[{}] PASSED - sensor_id: {}, status: {}", 
                    getName(), sensorId, status);
            return true;
            
        } catch (Exception e) {
            context.setFailureReason("Redis error: " + e.getMessage());
            context.setFailureStep(getName());
            log.error("[{}] FAILED - sensor_id: {}, Redis error: {}", 
                    getName(), sensorId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String getName() {
        return "IdentityStateValidator";
    }
}
