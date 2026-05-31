package org.project.floodalert.ingestion.validation.impl;

import com.github.benmanes.caffeine.cache.Cache;
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
    private final Cache<String, Map<String, String>> sensorIdentityCache;

    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "TESTING");

    @Override
    public boolean validate(ValidationContext context) {
        String sensorId = context.getSensorId();

        try {
            Map<String, String> sensorInfo = sensorIdentityCache.get(sensorId, this::loadFromRedis);

            if (sensorInfo == null || sensorInfo.isEmpty()) {
                context.setFailureReason("Sensor not registered");
                context.setFailureStep(getName());
                log.warn("[{}] FAILED - sensor_id: {} not found", getName(), sensorId);
                return false;
            }

            context.setSensorInfo(sensorInfo);

            String payloadApiKey = context.getSensorData().getDeviceInfo().getApiKey();
            String cachedApiKey = sensorInfo.get("api_key");

            if (payloadApiKey == null || !payloadApiKey.equals(cachedApiKey)) {
                context.setFailureReason("API key mismatch");
                context.setFailureStep(getName());
                log.warn("[{}] FAILED - sensor_id: {}, api_key mismatch", getName(), sensorId);
                return false;
            }

            String status = sensorInfo.get("status");

            if (status == null || !ALLOWED_STATUSES.contains(status.toUpperCase())) {
                context.setFailureReason(String.format("Invalid status: %s (allowed: %s)", status, ALLOWED_STATUSES));
                context.setFailureStep(getName());
                log.warn("[{}] FAILED - sensor_id: {}, status: {}", getName(), sensorId, status);
                return false;
            }

            log.debug("[{}] PASSED - sensor_id: {}, status: {}", getName(), sensorId, status);
            return true;

        } catch (Exception e) {
            context.setFailureReason("Validation error: " + e.getMessage());
            context.setFailureStep(getName());
            log.error("[{}] FAILED - sensor_id: {}, error: {}", getName(), sensorId, e.getMessage(), e);
            return false;
        }
    }

    private Map<String, String> loadFromRedis(String sensorId) {
        String key = validationProperties.getSensorInfoKeyPrefix() + sensorId;
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        if (raw.isEmpty()) return null;
        return raw.entrySet().stream()
                .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
    }

    @Override
    public String getName() {
        return "IdentityStateValidator";
    }
}
