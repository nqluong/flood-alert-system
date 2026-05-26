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
import org.project.floodalert.floodcore.dto.request.VirtualSensorProvisionRequest;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.project.floodalert.floodcore.service.CacheService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VirtualSensorServiceImplTest {

    @Mock private SensorRepository sensorRepository;
    @Mock private CacheService cacheService;
    @Mock private RedisKeyProperties redisKeyProperties;

    @InjectMocks
    private VirtualSensorServiceImpl virtualSensorService;

    private RedisKeyProperties.SensorKeys mockSensorKeys;

    @BeforeEach
    void setUp() {
        RedisKeyProperties.Keys keys = mock(RedisKeyProperties.Keys.class);
        mockSensorKeys = mock(RedisKeyProperties.SensorKeys.class);
        when(redisKeyProperties.getKeys()).thenReturn(keys);
        when(keys.getSensor()).thenReturn(mockSensorKeys);
        when(mockSensorKeys.getInfoKey(anyString())).thenReturn("sensor:info:VIRTUAL_0001");
    }

    private Sensor buildVirtualSensor() {
        return Sensor.builder()
                .id(UUID.randomUUID())
                .sensorId("VIRTUAL_0001")
                .name("Virtual Sensor 1")
                .locationName("Hanoi Virtual Location 1")
                .lat(new BigDecimal("21.00000000"))
                .lon(new BigDecimal("105.80000000"))
                .status("ACTIVE")
                .apiKey("test-api-key")
                .warningThreshold(new BigDecimal("20.0"))
                .dangerThreshold(new BigDecimal("30.0"))
                .isVirtual(true)
                .build();
    }

    // --- provisionVirtualSensors ---

    @Test
    void provisionVirtualSensors_alreadySufficient_noCreation() {
        when(sensorRepository.countByIsVirtual(true)).thenReturn(5L);
        when(sensorRepository.findByIsVirtual(true)).thenReturn(List.of(buildVirtualSensor()));
        virtualSensorService.provisionVirtualSensors(
                VirtualSensorProvisionRequest.builder().targetCount(3).build());
    }

    @Test
    void provisionVirtualSensors_exactCount_noCreation() {
        when(sensorRepository.countByIsVirtual(true)).thenReturn(3L);
        when(sensorRepository.findByIsVirtual(true)).thenReturn(List.of(buildVirtualSensor()));
        virtualSensorService.provisionVirtualSensors(
                VirtualSensorProvisionRequest.builder().targetCount(3).build());
    }

    @Test
    void provisionVirtualSensors_needsCreation_emptyExistingIds() {
        when(sensorRepository.countByIsVirtual(true)).thenReturn(0L);
        when(sensorRepository.findAllVirtualSensorIds()).thenReturn(List.of());
        when(sensorRepository.saveAll(any())).thenAnswer(inv -> new ArrayList<>(List.of(buildVirtualSensor())));
        when(sensorRepository.findByIsVirtual(true)).thenReturn(List.of(buildVirtualSensor()));
        virtualSensorService.provisionVirtualSensors(
                VirtualSensorProvisionRequest.builder().targetCount(2).build());
    }

    @Test
    void provisionVirtualSensors_needsCreation_withExistingIds() {
        when(sensorRepository.countByIsVirtual(true)).thenReturn(1L);
        when(sensorRepository.findAllVirtualSensorIds()).thenReturn(List.of("VIRTUAL_0001"));
        when(sensorRepository.saveAll(any())).thenAnswer(inv -> new ArrayList<>(List.of(buildVirtualSensor())));
        when(sensorRepository.findByIsVirtual(true)).thenReturn(List.of(buildVirtualSensor()));
        virtualSensorService.provisionVirtualSensors(
                VirtualSensorProvisionRequest.builder().targetCount(3).build());
    }

    @Test
    void provisionVirtualSensors_needsCreation_nonNumericIds_filteredOut() {
        when(sensorRepository.countByIsVirtual(true)).thenReturn(0L);
        when(sensorRepository.findAllVirtualSensorIds()).thenReturn(List.of("VIRTUAL_ABC", "VIRTUAL_XYZ"));
        when(sensorRepository.saveAll(any())).thenAnswer(inv -> new ArrayList<>(List.of(buildVirtualSensor())));
        when(sensorRepository.findByIsVirtual(true)).thenReturn(List.of(buildVirtualSensor()));
        virtualSensorService.provisionVirtualSensors(
                VirtualSensorProvisionRequest.builder().targetCount(1).build());
    }

    @Test
    void provisionVirtualSensors_needsCreation_multiBatch() {
        when(sensorRepository.countByIsVirtual(true)).thenReturn(0L);
        when(sensorRepository.findAllVirtualSensorIds()).thenReturn(List.of());
        when(sensorRepository.saveAll(any())).thenAnswer(inv -> new ArrayList<>(List.of(buildVirtualSensor())));
        when(sensorRepository.findByIsVirtual(true)).thenReturn(List.of(buildVirtualSensor()));
        virtualSensorService.provisionVirtualSensors(
                VirtualSensorProvisionRequest.builder().targetCount(600).build());
    }

    @Test
    void provisionVirtualSensors_syncRedisException_swallowed() {
        when(sensorRepository.countByIsVirtual(true)).thenReturn(0L);
        when(sensorRepository.findAllVirtualSensorIds()).thenReturn(List.of());
        when(sensorRepository.saveAll(any())).thenAnswer(inv -> new ArrayList<>(List.of(buildVirtualSensor())));
        when(sensorRepository.findByIsVirtual(true)).thenReturn(List.of(buildVirtualSensor()));
        doThrow(new RuntimeException("redis error")).when(cacheService).batchHSetFullKeys(any());
        virtualSensorService.provisionVirtualSensors(
                VirtualSensorProvisionRequest.builder().targetCount(2).build());
    }

    // --- cleanupVirtualSensors ---

    @Test
    void cleanupVirtualSensors_noSensors_returnsZero() {
        when(sensorRepository.findByIsVirtual(true)).thenReturn(List.of());
        virtualSensorService.cleanupVirtualSensors();
    }

    @Test
    void cleanupVirtualSensors_withSensors_success() {
        when(sensorRepository.findByIsVirtual(true)).thenReturn(List.of(buildVirtualSensor()));
        virtualSensorService.cleanupVirtualSensors();
    }

    @Test
    void cleanupVirtualSensors_removeFromRedisException_swallowed() {
        when(sensorRepository.findByIsVirtual(true)).thenReturn(List.of(buildVirtualSensor()));
        doThrow(new RuntimeException("redis error")).when(cacheService).batchDelete(any());
        virtualSensorService.cleanupVirtualSensors();
    }
}
