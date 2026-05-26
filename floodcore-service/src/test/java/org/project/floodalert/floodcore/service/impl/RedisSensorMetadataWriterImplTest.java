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
import org.project.floodalert.floodcore.mapper.SensorMetadataMapper;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.service.CacheService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisSensorMetadataWriterImplTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private SensorMetadataMapper metadataMapper;

    @Mock
    private RedisKeyProperties redisKeyProperties;

    @InjectMocks
    private RedisSensorMetadataWriterImpl redisSensorMetadataWriterImpl;

    @BeforeEach
    void setUp() {
        RedisKeyProperties.Keys keys = mock(RedisKeyProperties.Keys.class);
        RedisKeyProperties.SensorKeys sensorKeys = mock(RedisKeyProperties.SensorKeys.class);

        when(redisKeyProperties.getKeys()).thenReturn(keys);
        when(keys.getSensor()).thenReturn(sensorKeys);
        // replace(":{sensorId}", ":") → keyPrefix = "sensor:info:"
        when(sensorKeys.getInfo()).thenReturn("sensor:info:{sensorId}");
    }

    @Test
    void batchWriteSensorMetadata_emptySensorList_callsBatchHSetWithEmptyMap() {
        redisSensorMetadataWriterImpl.batchWriteSensorMetadata(List.of());
    }

    @Test
    void batchWriteSensorMetadata_withSingleSensor_callsBatchHSet() {
        Sensor sensor = Sensor.builder()
                .sensorId("S001")
                .name("Cảm biến Test")
                .status("ACTIVE")
                .build();

        when(metadataMapper.toMetadataMap(sensor))
                .thenReturn(Map.of("status", "ACTIVE", "name", "Cảm biến Test"));

        redisSensorMetadataWriterImpl.batchWriteSensorMetadata(List.of(sensor));
    }

    @Test
    void batchWriteSensorMetadata_withMultipleSensors_callsBatchHSet() {
        Sensor s1 = Sensor.builder().sensorId("S001").status("ACTIVE").build();
        Sensor s2 = Sensor.builder().sensorId("S002").status("DISABLED").build();

        when(metadataMapper.toMetadataMap(s1)).thenReturn(Map.of("status", "ACTIVE"));
        when(metadataMapper.toMetadataMap(s2)).thenReturn(Map.of("status", "DISABLED"));

        redisSensorMetadataWriterImpl.batchWriteSensorMetadata(List.of(s1, s2));
    }
}
