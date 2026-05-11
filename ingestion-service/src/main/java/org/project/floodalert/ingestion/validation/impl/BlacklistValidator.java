package org.project.floodalert.ingestion.validation.impl;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.config.ValidationProperties;
import org.project.floodalert.ingestion.validation.SensorDataValidator;
import org.project.floodalert.ingestion.validation.ValidationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistValidator implements SensorDataValidator {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValidationProperties validationProperties;
    private final Cache<String, Boolean> sensorBlacklistCache;
    
    @Override
    public boolean validate(ValidationContext context) {
        String sensorId = context.getSensorId();
        
        // Check cache first
        Boolean cachedResult = sensorBlacklistCache.getIfPresent(sensorId);
        if (cachedResult != null) {
            log.debug("[{}] Cache HIT for sensor_id: {}, blacklisted: {}", 
                    getName(), sensorId, cachedResult);
            
            if (cachedResult) {
                context.setFailureReason("Sensor is blacklisted (cached)");
                context.setFailureStep(getName());
                log.warn("[{}] FAILED - sensor_id: {} is in blacklist (cached)", 
                        getName(), sensorId);
                return false;
            }
            return true;
        }
        
        // Cache miss - check Redis
        log.debug("[{}] Cache MISS for sensor_id: {}, checking Redis", 
                getName(), sensorId);
        
        String blacklistKey = validationProperties.getBlacklistKeyPrefix() + sensorId;
        
        try {
            Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
            boolean blacklisted = Boolean.TRUE.equals(isBlacklisted);
            
            // Cache the result
            sensorBlacklistCache.put(sensorId, blacklisted);
            
            if (blacklisted) {
                context.setFailureReason("Sensor is blacklisted");
                context.setFailureStep(getName());
                log.warn("[{}] FAILED - sensor_id: {} is in blacklist", 
                        getName(), sensorId);
                return false;
            }
            
            log.debug("[{}] PASSED - sensor_id: {} not in blacklist", 
                    getName(), sensorId);
            return true;
            
        } catch (Exception e) {
            // Fail-open: Nếu Redis lỗi, cho phép sensor đi tiếp
            log.error("[{}] Redis error for sensor_id: {}, allowing sensor to proceed. Error: {}", 
                    getName(), sensorId, e.getMessage());
            
            // Cache negative result để tránh spam Redis khi lỗi
            sensorBlacklistCache.put(sensorId, false);
            return true;
        }
    }
    
    @Override
    public String getName() {
        return "BlacklistValidator";
    }
    
    /**
     * Invalidate cache entry cho một sensor cụ thể
     * Dùng khi sensor được thêm/xóa khỏi blacklist
     */
    public void invalidateCacheEntry(String sensorId) {
        sensorBlacklistCache.invalidate(sensorId);
        log.info("[{}] Cache invalidated for sensor_id: {}", getName(), sensorId);
    }
    
    /**
     * Clear toàn bộ cache
     */
    public void clearCache() {
        sensorBlacklistCache.invalidateAll();
        log.info("[{}] Blacklist cache cleared", getName());
    }
}
