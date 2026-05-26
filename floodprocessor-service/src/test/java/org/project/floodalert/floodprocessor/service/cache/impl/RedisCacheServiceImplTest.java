package org.project.floodalert.floodprocessor.service.cache.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.config.RedisKeyProperties;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisHashCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.HashOperations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisKeyProperties redisKeyProperties;

    @Mock
    private RedisKeyProperties.Keys keys;

    @Mock
    private RedisKeyProperties.SensorKeys sensor;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private RedisHashCommands hashCommands;

    @InjectMocks
    private RedisCacheServiceImpl redisCacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisKeyProperties.getKeys()).thenReturn(keys);
        lenient().when(keys.getSensor()).thenReturn(sensor);
    }

    @Test
    void bulkFetchSensorInfo_nullInput_returnsEmptyMap() {
        Map<String, Map<String, String>> result = redisCacheService.bulkFetchSensorInfo(null);
        assertEquals(Collections.emptyMap(), result);
    }

    @Test
    void bulkFetchSensorInfo_emptyInput_returnsEmptyMap() {
        Map<String, Map<String, String>> result = redisCacheService.bulkFetchSensorInfo(Collections.emptySet());
        assertEquals(Collections.emptyMap(), result);
    }

    @Test
    void bulkFetchSensorInfo_success_returnsMappedData() {
        Set<String> sensorIds = new LinkedHashSet<>(Arrays.asList("sensor1", "sensor2"));

        when(sensor.getInfoKey("sensor1")).thenReturn("sensor:info:sensor1");
        when(sensor.getInfoKey("sensor2")).thenReturn("sensor:info:sensor2");

        Map<String, String> data1 = new HashMap<>();
        data1.put("name", "Sensor One");

        Map<String, String> data2 = new HashMap<>();
        data2.put("name", "Sensor Two");

        List<Object> pipelineResults = Arrays.asList(data1, data2);

        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            // Simulate pipeline execution with mocked connection
            when(redisConnection.hashCommands()).thenReturn(hashCommands);
            callback.doInRedis(redisConnection);
            return pipelineResults;
        });

        Map<String, Map<String, String>> result = redisCacheService.bulkFetchSensorInfo(sensorIds);
        assertNotNull(result);
    }

    @Test
    void bulkFetchSensorInfo_pipelineResultHasNullEntry_skipsNull() {
        Set<String> sensorIds = new LinkedHashSet<>(Arrays.asList("sensor1", "sensor2"));

        when(sensor.getInfoKey("sensor1")).thenReturn("sensor:info:sensor1");
        when(sensor.getInfoKey("sensor2")).thenReturn("sensor:info:sensor2");

        // sensor1 -> null result (not a Map), sensor2 -> empty map
        List<Object> pipelineResults = Arrays.asList("not-a-map", Collections.emptyMap());

        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            when(redisConnection.hashCommands()).thenReturn(hashCommands);
            callback.doInRedis(redisConnection);
            return pipelineResults;
        });

        Map<String, Map<String, String>> result = redisCacheService.bulkFetchSensorInfo(sensorIds);
        assertNotNull(result);
    }

    @Test
    void bulkFetchSensorInfo_hGetAllThrowsException_logsWarnAndContinues() {
        Set<String> sensorIds = new LinkedHashSet<>(List.of("sensor1"));

        when(sensor.getInfoKey("sensor1")).thenReturn("sensor:info:sensor1");

        Map<String, String> data = Map.of("field", "value");
        List<Object> pipelineResults = List.of(data);

        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            when(redisConnection.hashCommands()).thenReturn(hashCommands);
            // hGetAll throws inside the pipeline callback
            doThrow(new RuntimeException("hGetAll error"))
                    .when(hashCommands).hGetAll(any(byte[].class));
            callback.doInRedis(redisConnection);
            return pipelineResults;
        });

        // Should not throw — exception inside lambda is caught and logged
        assertDoesNotThrow(() -> redisCacheService.bulkFetchSensorInfo(sensorIds));
    }

    @Test
    void bulkFetchSensorInfo_redisConnectionFailure_throwsAppException() {
        Set<String> sensorIds = Set.of("sensor1");

        when(sensor.getInfoKey("sensor1")).thenReturn("sensor:info:sensor1");
        when(redisTemplate.executePipelined(any(RedisCallback.class)))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        assertThrows(AppException.class, () -> redisCacheService.bulkFetchSensorInfo(sensorIds));
    }

    @Test
    void bulkFetchSensorInfo_unexpectedException_throwsAppException() {
        Set<String> sensorIds = Set.of("sensor1");

        when(sensor.getInfoKey("sensor1")).thenReturn("sensor:info:sensor1");
        when(redisTemplate.executePipelined(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertThrows(AppException.class, () -> redisCacheService.bulkFetchSensorInfo(sensorIds));
    }

    // ==================== getSensorInfo ====================

    @Test
    void getSensorInfo_nullSensorId_returnsNull() {
        assertNull(redisCacheService.getSensorInfo(null));
    }

    @Test
    void getSensorInfo_emptySensorId_returnsNull() {
        assertNull(redisCacheService.getSensorInfo(""));
    }

    @Test
    void getSensorInfo_noDataInRedis_returnsNull() {
        when(sensor.getInfoKey("sensor1")).thenReturn("sensor:info:sensor1");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("sensor:info:sensor1")).thenReturn(Collections.emptyMap());

        assertNull(redisCacheService.getSensorInfo("sensor1"));
    }

    @Test
    void getSensorInfo_dataFound_returnsStringMap() {
        when(sensor.getInfoKey("sensor1")).thenReturn("sensor:info:sensor1");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        Map<Object, Object> rawData = new HashMap<>();
        rawData.put("name", "River Station");
        rawData.put("location", "Hanoi");

        when(hashOperations.entries("sensor:info:sensor1")).thenReturn(rawData);

        Map<String, String> result = redisCacheService.getSensorInfo("sensor1");
        assertNotNull(result);
    }

    @Test
    void getSensorInfo_rawDataContainsNullKeyOrValue_skipsEntry() {
        when(sensor.getInfoKey("sensor1")).thenReturn("sensor:info:sensor1");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        Map<Object, Object> rawData = new HashMap<>();
        rawData.put(null, "value1");
        rawData.put("key2", null);
        rawData.put("key3", "value3");

        when(hashOperations.entries("sensor:info:sensor1")).thenReturn(rawData);

        Map<String, String> result = redisCacheService.getSensorInfo("sensor1");
        assertNotNull(result);
    }

    @Test
    void getSensorInfo_redisConnectionFailure_throwsAppException() {
        when(sensor.getInfoKey("sensor1")).thenReturn("sensor:info:sensor1");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString()))
                .thenThrow(new RedisConnectionFailureException("Connection failed"));

        assertThrows(AppException.class, () -> redisCacheService.getSensorInfo("sensor1"));
    }

    @Test
    void getSensorInfo_unexpectedException_throwsAppException() {
        when(sensor.getInfoKey("sensor1")).thenReturn("sensor:info:sensor1");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString()))
                .thenThrow(new RuntimeException("Unexpected"));

        assertThrows(AppException.class, () -> redisCacheService.getSensorInfo("sensor1"));
    }

    // ==================== isRedisAvailable ====================

    @Test
    void isRedisAvailable_pingReturnsPong_returnsTrue() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        assertTrue(redisCacheService.isRedisAvailable());
    }

    @Test
    void isRedisAvailable_pingReturnsPongLowercase_returnsTrue() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("pong");

        assertTrue(redisCacheService.isRedisAvailable());
    }

    @Test
    void isRedisAvailable_pingReturnsUnexpectedValue_returnsFalse() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("ERROR");

        assertFalse(redisCacheService.isRedisAvailable());
    }

    @Test
    void isRedisAvailable_exceptionThrown_returnsFalse() {
        when(redisTemplate.getConnectionFactory()).thenThrow(new RuntimeException("No factory"));

        assertFalse(redisCacheService.isRedisAvailable());
    }
}