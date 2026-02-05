package org.project.floodalert.ingestion.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.config.RedisKeyProperties;
import org.project.floodalert.ingestion.service.SensorBlacklistService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorBlacklistServiceImpl implements SensorBlacklistService {

    private final RedisKeyProperties redisKeyProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<String, Boolean> sensorBlacklistCache;

    @Override
    public boolean isSensorBlacklisted(String sensorId) {

        Boolean cachedResult = (Boolean) sensorBlacklistCache.getIfPresent(sensorId);
        if(cachedResult != null){
            log.debug("Cache HIT for sensor_id: {}", sensorId);
            return cachedResult;
        }
        boolean isBlacklisted = checkBlacklistInRedis(sensorId);
        sensorBlacklistCache.put(sensorId, isBlacklisted);

        return isBlacklisted;
    }

    private boolean checkBlacklistInRedis(String sensorId) {
        try {
            Boolean member = redisTemplate.opsForSet()
                    .isMember(redisKeyProperties.getKeys().getSensor().getBlacklist(), sensorId);
            return Boolean.TRUE.equals(member);

        } catch (Exception e) {
            log.warn("Redis connection failed while checking blacklist for sensor_id: {}. " +
                    "Allowing sensor to proceed. Error: {}", sensorId, e.getMessage());
            return false;
        }
    }

    public void invalidateCacheEntry(String sensorId) {
        sensorBlacklistCache.invalidate(sensorId);
        log.info("Cache invalidated for sensor_id: {}", sensorId);
    }

    public void clearCache() {
        sensorBlacklistCache.invalidateAll();
        log.info("Sensor blacklist cache cleared");
    }

    public CacheStats getCacheStats() {
        return sensorBlacklistCache.stats();
    }
}
