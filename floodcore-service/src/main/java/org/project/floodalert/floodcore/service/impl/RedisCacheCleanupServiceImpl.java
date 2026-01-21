package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.service.CacheCleanupService;
import org.project.floodalert.floodcore.service.CacheService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheCleanupServiceImpl implements CacheCleanupService {

    private final CacheService cacheService;
    private static final String GEO_ALL_SENSORS_KEY = "maps:all_sensors";

    /**
     * Xóa dữ liệu geo-location cũ của sensors
     */
    @Override
    public void cleanupSensorGeoData() {
        Boolean deleted = cacheService.delete(GEO_ALL_SENSORS_KEY);
        log.debug("[CLEANUP] Đã xóa dữ liệu geo cũ. Kết quả: {}", deleted);
    }
}
