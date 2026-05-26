package org.project.floodalert.floodcore.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.service.CacheCleanupService;
import org.project.floodalert.floodcore.service.SensorBlacklistWriter;
import org.project.floodalert.floodcore.service.SensorMetadataWriter;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class RedisSensorSyncServiceImplTest {

    @Mock
    private SensorMetadataWriter metadataWriter;

    @Mock
    private SensorBlacklistWriter blacklistWriter;

    @Mock
    private CacheCleanupService cleanupService;

    @InjectMocks
    private RedisSensorSyncServiceImpl redisSensorSyncService;

    @Test
    void syncSensorsToCache_emptySensorList() {
        redisSensorSyncService.syncSensorsToCache(List.of());
    }

    @Test
    void syncSensorsToCache_withSensors() {
        List<Sensor> sensors = List.of(
                Sensor.builder().sensorId("S001").status("ACTIVE").build(),
                Sensor.builder().sensorId("S002").status("DISABLED").build()
        );
        redisSensorSyncService.syncSensorsToCache(sensors);
    }
}
