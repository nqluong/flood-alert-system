package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.config.RedisKeyProperties;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.service.CacheService;
import org.project.floodalert.floodcore.service.SensorBlacklistWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSensorBlacklistWriterImpl implements SensorBlacklistWriter {
    private final CacheService cacheService;
    private final RedisKeyProperties redisKeyProperties;

    /**
     * Ghi danh sách sensor bị vô hiệu hóa (blacklist) vào Redis Set
     */
    @Override
    public void batchWriteSensorBlacklist(List<Sensor> sensors) {
        String blacklistKey = redisKeyProperties.getKeys().getSensor().getBlacklist();
        
        // Lọc các sensor bị disabled (status khác ACTIVE)
        List<String> disabledSensorIds = sensors.stream()
                .filter(sensor -> !"ACTIVE".equalsIgnoreCase(sensor.getStatus()))
                .map(Sensor::getSensorId)
                .toList();

        if (disabledSensorIds.isEmpty()) {
            log.info("[BLACKLIST] Không có sensor nào bị vô hiệu hóa");
            return;
        }

        // Thêm vào Redis Set
        Long added = cacheService.sAdd(blacklistKey, disabledSensorIds.toArray());
        
        log.info("[BLACKLIST] Đã cache {} sensors bị vô hiệu hóa vào blacklist", added);
    }
}
