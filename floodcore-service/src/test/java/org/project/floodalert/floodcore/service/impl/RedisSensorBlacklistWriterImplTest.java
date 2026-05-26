package org.project.floodalert.floodcore.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.project.floodalert.floodcore.config.RedisKeyProperties;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.service.CacheService;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisSensorBlacklistWriterImplTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private RedisKeyProperties redisKeyProperties;

    @InjectMocks
    private RedisSensorBlacklistWriterImpl redisSensorBlacklistWriter;

    @BeforeEach
    void setUp() {
        RedisKeyProperties.Keys keys = mock(RedisKeyProperties.Keys.class);
        RedisKeyProperties.SensorKeys sensorKeys = mock(RedisKeyProperties.SensorKeys.class);

        when(redisKeyProperties.getKeys()).thenReturn(keys);
        when(keys.getSensor()).thenReturn(sensorKeys);
        when(sensorKeys.getBlacklist()).thenReturn("sensor:blacklist");
    }

    @Test
    void batchWriteSensorBlacklist_emptySensorList_noSAddCall() {
        redisSensorBlacklistWriter.batchWriteSensorBlacklist(List.of());
    }

    @Test
    void batchWriteSensorBlacklist_allActiveSensors_returnsEarly() {
        List<Sensor> sensors = List.of(
                Sensor.builder().sensorId("S001").status("ACTIVE").build(),
                Sensor.builder().sensorId("S002").status("active").build()
        );

        redisSensorBlacklistWriter.batchWriteSensorBlacklist(sensors);
    }

    @Test
    void batchWriteSensorBlacklist_withDisabledSensors_callsSAdd() {
        List<Sensor> sensors = List.of(
                Sensor.builder().sensorId("S001").status("ACTIVE").build(),
                Sensor.builder().sensorId("S002").status("DISABLED").build(),
                Sensor.builder().sensorId("S003").status("OFFLINE").build(),
                Sensor.builder().sensorId("S004").status("MAINTENANCE").build()
        );

        redisSensorBlacklistWriter.batchWriteSensorBlacklist(sensors);
    }
}
