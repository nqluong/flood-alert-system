package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.service.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSensorSyncServiceImpl implements SensorSyncService {
    private final SensorMetadataWriter metadataWriter;
    private final SensorBlacklistWriter blacklistWriter;
    private final CacheCleanupService cleanupService;

    /**
     * Đồng bộ danh sách sensors lên Redis cache
     */
    @Override
    public void syncSensorsToCache(List<Sensor> sensors) {
        //  Xóa dữ liệu cũ để tránh rác
        cleanupService.cleanupSensorBlacklist();

        // Ghi metadata và blacklist sử dụng pipeline
        metadataWriter.batchWriteSensorMetadata(sensors);
        blacklistWriter.batchWriteSensorBlacklist(sensors);

        log.info("[SYNC] Hoàn tất đồng bộ {} sensors lên Redis", sensors.size());
    }
}
