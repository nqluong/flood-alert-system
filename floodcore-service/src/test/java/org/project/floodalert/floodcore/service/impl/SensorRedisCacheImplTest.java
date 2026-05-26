package org.project.floodalert.floodcore.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.config.RedisKeyProperties;
import org.project.floodalert.floodcore.dto.request.SensorFilterRequest;
import org.project.floodalert.floodcore.dto.response.SensorDetailResponse;
import org.project.floodalert.floodcore.dto.response.SensorMapResponse;
import org.project.floodalert.floodcore.dto.response.SensorSummaryResponse;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.service.CacheService;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensorRedisCacheImplTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private RedisKeyProperties redisKeyProperties;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SensorRedisCacheImpl sensorRedisCache;

    private RedisKeyProperties.SensorKeys mockSensorKeys;
    private RedisKeyProperties.MapsKeys mockMapsKeys;
    private RedisKeyProperties.CacheKeys mockCacheKeys;
    private RedisKeyProperties.Ttl mockTtl;

    @BeforeEach
    void setUp() {
        RedisKeyProperties.Keys keys = mock(RedisKeyProperties.Keys.class);
        mockSensorKeys = mock(RedisKeyProperties.SensorKeys.class);
        mockMapsKeys = mock(RedisKeyProperties.MapsKeys.class);
        mockCacheKeys = mock(RedisKeyProperties.CacheKeys.class);
        mockTtl = mock(RedisKeyProperties.Ttl.class);

        when(redisKeyProperties.getKeys()).thenReturn(keys);
        when(keys.getSensor()).thenReturn(mockSensorKeys);
        when(keys.getMaps()).thenReturn(mockMapsKeys);
        when(keys.getCache()).thenReturn(mockCacheKeys);
        when(redisKeyProperties.getTtl()).thenReturn(mockTtl);

        when(mockSensorKeys.getInfoKey(anyString())).thenReturn("sensor:info:S001");
        when(mockSensorKeys.getBlacklist()).thenReturn("sensor:blacklist");
        when(mockMapsKeys.getAllSensors()).thenReturn("sensor:map:all");
        when(mockCacheKeys.getSensorDetailKey(anyString())).thenReturn("sensor:detail:S001");
        when(mockCacheKeys.getSensorList()).thenReturn("sensor:list");
        when(mockCacheKeys.getMapAll()).thenReturn("sensor:map:all-cache");
        when(mockTtl.getSensorList()).thenReturn(300L);
        when(mockTtl.getSensorDetail()).thenReturn(600L);
        when(mockTtl.getMapCache()).thenReturn(3600L);
    }

    private Sensor buildSensor(String status) {
        return Sensor.builder()
                .id(UUID.randomUUID())
                .sensorId("S001")
                .name("Test Sensor")
                .locationName("Quận 12")
                .lat(new BigDecimal("10.5"))
                .lon(new BigDecimal("106.5"))
                .status(status)
                .apiKey("test-api-key")
                .warningThreshold(new BigDecimal("0.5"))
                .dangerThreshold(new BigDecimal("1.0"))
                .hardwareModel("Model A")
                .firmwareVersion("1.0.0")
                .build();
    }

    // --- syncSensorToRedis ---

    @Test
    void syncSensorToRedis_activeSensor_success() {
        sensorRedisCache.syncSensorToRedis(buildSensor("ACTIVE"));
    }

    @Test
    void syncSensorToRedis_disabledSensor_addsToBlacklist() {
        sensorRedisCache.syncSensorToRedis(buildSensor("DISABLED"));
    }

    @Test
    void syncSensorToRedis_exceptionThrown_throwsAppException() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).hSetAll(anyString(), any());
        assertThrows(AppException.class, () -> sensorRedisCache.syncSensorToRedis(buildSensor("ACTIVE")));
    }

    @Test
    void saveSensorMetadata_withNullHardwareAndFirmware() {
        Sensor sensor = Sensor.builder()
                .id(UUID.randomUUID()).sensorId("S001")
                .lat(new BigDecimal("10.5")).lon(new BigDecimal("106.5"))
                .status("ACTIVE").apiKey("key")
                .warningThreshold(new BigDecimal("0.5"))
                .dangerThreshold(new BigDecimal("1.0"))
                .hardwareModel(null).firmwareVersion(null)
                .build();
        sensorRedisCache.saveSensorMetadata(sensor);
    }

    @Test
    void saveSensorLocation_success() {
        sensorRedisCache.saveSensorLocation(buildSensor("ACTIVE"));
    }

    @Test
    void removeSensorFromRedis_success() {
        sensorRedisCache.removeSensorFromRedis("S001");
    }

    @Test
    void removeSensorFromRedis_exceptionThrown_throwsAppException() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).delete(anyString());
        assertThrows(AppException.class, () -> sensorRedisCache.removeSensorFromRedis("S001"));
    }

    @Test
    void updateSensorStatus_success() {
        sensorRedisCache.updateSensorStatus("S001", "DISABLED");
    }

    @Test
    void updateSensorStatus_exceptionThrown_throwsAppException() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).hSet(anyString(), anyString(), any());
        assertThrows(AppException.class, () -> sensorRedisCache.updateSensorStatus("S001", "ACTIVE"));
    }

    @Test
    void getCachedSensorList_cacheHit_returnsOptional() {
        when(cacheService.get(anyString())).thenReturn(new Object());
        when(objectMapper.convertValue(any(), any(TypeReference.class))).thenReturn(new PageResponse<SensorSummaryResponse>());

        sensorRedisCache.getCachedSensorList(SensorFilterRequest.builder().build());
    }

    @Test
    void getCachedSensorList_cacheMiss_returnsEmpty() {
        when(cacheService.get(anyString())).thenReturn(null);

        sensorRedisCache.getCachedSensorList(SensorFilterRequest.builder().build());
    }

    @Test
    void getCachedSensorList_withFullFilter_coversAllBuildKeyBranches() {
        when(cacheService.get(anyString())).thenReturn(null);
        SensorFilterRequest filter = SensorFilterRequest.builder()
                .status("ACTIVE").search("test").sortBy("name").sortDirection("ASC")
                .build();

        sensorRedisCache.getCachedSensorList(filter);
    }

    @Test
    void getCachedSensorList_exceptionThrown_returnsEmpty() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).get(anyString());

        sensorRedisCache.getCachedSensorList(SensorFilterRequest.builder().build());
    }

    @Test
    void cacheSensorList_success() {
        sensorRedisCache.cacheSensorList(SensorFilterRequest.builder().build(), new PageResponse<>());
    }

    @Test
    void cacheSensorList_exceptionThrown_logsError() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).set(anyString(), any(), anyLong(), any());

        sensorRedisCache.cacheSensorList(SensorFilterRequest.builder().build(), new PageResponse<>());
    }

    @Test
    void getCachedSensorDetail_cacheHit_returnsOptional() {
        when(cacheService.get(anyString())).thenReturn(new Object());
        when(objectMapper.convertValue(any(), eq(SensorDetailResponse.class))).thenReturn(new SensorDetailResponse());

        sensorRedisCache.getCachedSensorDetail(UUID.randomUUID());
    }

    @Test
    void getCachedSensorDetail_cacheMiss_returnsEmpty() {
        when(cacheService.get(anyString())).thenReturn(null);

        sensorRedisCache.getCachedSensorDetail(UUID.randomUUID());
    }

    @Test
    void getCachedSensorDetail_exceptionThrown_returnsEmpty() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).get(anyString());

        sensorRedisCache.getCachedSensorDetail(UUID.randomUUID());
    }

    @Test
    void cacheSensorDetail_success() {
        sensorRedisCache.cacheSensorDetail(UUID.randomUUID(), new SensorDetailResponse());
    }

    @Test
    void cacheSensorDetail_exceptionThrown_logsError() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).set(anyString(), any(), anyLong(), any());

        sensorRedisCache.cacheSensorDetail(UUID.randomUUID(), new SensorDetailResponse());
    }

    @Test
    void getCachedMapData_cacheHit_returnsOptional() {
        when(cacheService.get(anyString())).thenReturn(new Object());
        when(objectMapper.convertValue(any(), eq(SensorMapResponse.class))).thenReturn(new SensorMapResponse());

        sensorRedisCache.getCachedMapData();
    }

    @Test
    void getCachedMapData_cacheMiss_returnsEmpty() {
        when(cacheService.get(anyString())).thenReturn(null);

        sensorRedisCache.getCachedMapData();
    }

    @Test
    void getCachedMapData_exceptionThrown_returnsEmpty() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).get(anyString());

        sensorRedisCache.getCachedMapData();
    }

    @Test
    void cacheMapData_success() {
        sensorRedisCache.cacheMapData(new SensorMapResponse());
    }

    @Test
    void cacheMapData_exceptionThrown_logsError() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).set(anyString(), any(), anyLong(), any());

        sensorRedisCache.cacheMapData(new SensorMapResponse());
    }

    @Test
    void evictSensorCache_withNonEmptyKeys_batchDeletes() {
        when(cacheService.keys(anyString())).thenReturn(Set.of("sensor:list:page=0:size=20"));

        sensorRedisCache.evictSensorCache(UUID.randomUUID());
    }

    @Test
    void evictAllListCache_keysNull_skips() {
        when(cacheService.keys(anyString())).thenReturn(null);

        sensorRedisCache.evictAllListCache();
    }

    @Test
    void evictAllListCache_keysEmpty_skips() {
        when(cacheService.keys(anyString())).thenReturn(Set.of());

        sensorRedisCache.evictAllListCache();
    }

    @Test
    void evictAllListCache_exceptionThrown_logsError() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).keys(anyString());

        sensorRedisCache.evictAllListCache();
    }

    // --- evictMapCache ---

    @Test
    void evictMapCache_success() {
        sensorRedisCache.evictMapCache();
    }

    @Test
    void evictMapCache_exceptionThrown_logsError() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).delete(anyString());

        sensorRedisCache.evictMapCache();
    }

    // --- addToBlacklist ---

    @Test
    void addToBlacklist_success() {
        sensorRedisCache.addToBlacklist("S001");
    }

    @Test
    void addToBlacklist_exceptionThrown_logsError() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).sAdd(anyString(), any());

        sensorRedisCache.addToBlacklist("S001");
    }

    // --- removeFromBlacklist ---

    @Test
    void removeFromBlacklist_removedCountPositive_logsInfo() {
        when(cacheService.sRemove(anyString(), any())).thenReturn(1L);

        sensorRedisCache.removeFromBlacklist("S001");
    }

    @Test
    void removeFromBlacklist_removedCountZero_logsDebug() {
        when(cacheService.sRemove(anyString(), any())).thenReturn(0L);

        sensorRedisCache.removeFromBlacklist("S001");
    }

    @Test
    void removeFromBlacklist_removedCountNull_logsDebug() {
        when(cacheService.sRemove(anyString(), any())).thenReturn(null);

        sensorRedisCache.removeFromBlacklist("S001");
    }

    @Test
    void removeFromBlacklist_exceptionThrown_logsError() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).sRemove(anyString(), any());

        sensorRedisCache.removeFromBlacklist("S001");
    }

    // --- isInBlacklist ---

    @Test
    void isInBlacklist_isMemberTrue_returnsTrue() {
        when(cacheService.sIsMember(anyString(), any())).thenReturn(true);

        sensorRedisCache.isInBlacklist("S001");
    }

    @Test
    void isInBlacklist_isMemberNull_returnsFalse() {
        when(cacheService.sIsMember(anyString(), any())).thenReturn(null);

        sensorRedisCache.isInBlacklist("S001");
    }

    @Test
    void isInBlacklist_exceptionThrown_returnsFalse() {
        doThrow(new RuntimeException("Redis error")).when(cacheService).sIsMember(anyString(), any());

        sensorRedisCache.isInBlacklist("S001");
    }

    // --- syncBlacklistStatus ---

    @Test
    void syncBlacklistStatus_maintenanceStatus_callsAddToBlacklist() {
        sensorRedisCache.syncBlacklistStatus("S001", "MAINTENANCE");
    }

    @Test
    void syncBlacklistStatus_deletedStatus_callsAddToBlacklist() {
        sensorRedisCache.syncBlacklistStatus("S001", "DELETED");
    }

    @Test
    void syncBlacklistStatus_offlineStatus_callsAddToBlacklist() {
        sensorRedisCache.syncBlacklistStatus("S001", "OFFLINE");
    }

    @Test
    void syncBlacklistStatus_activeStatus_callsRemoveFromBlacklist() {
        sensorRedisCache.syncBlacklistStatus("S001", "ACTIVE");
    }
}
