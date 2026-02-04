package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.config.RedisKeyProperties;
import org.project.floodalert.floodcore.service.CacheCleanupService;
import org.project.floodalert.floodcore.service.CacheService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheCleanupServiceImpl implements CacheCleanupService {

    private final CacheService cacheService;
    private final RedisKeyProperties redisKeyProperties;

    /**
     * Xóa dữ liệu blacklist cũ của sensors
     */
    @Override
    public void cleanupSensorBlacklist() {
        String blacklistKey = redisKeyProperties.getKeys().getSensor().getBlacklist();
        Boolean deleted = cacheService.delete(blacklistKey);
    }
}
