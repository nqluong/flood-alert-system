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
import org.project.floodalert.floodcore.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodcore.service.CacheService;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisFloodGeoCacheImplTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private RedisKeyProperties redisKeyProperties;

    @InjectMocks
    private RedisFloodGeoCacheImpl redisFloodGeoCache;

    @BeforeEach
    void setUp() {
        RedisKeyProperties.Keys keys = mock(RedisKeyProperties.Keys.class);
        RedisKeyProperties.FloodKeys floodKeys = mock(RedisKeyProperties.FloodKeys.class);
        RedisKeyProperties.Ttl ttl = mock(RedisKeyProperties.Ttl.class);

        when(redisKeyProperties.getKeys()).thenReturn(keys);
        when(keys.getFlood()).thenReturn(floodKeys);
        when(floodKeys.getGeoIndex()).thenReturn("flood:geo");
        when(floodKeys.getDetailKey(anyString())).thenReturn("flood:detail:test");
        when(redisKeyProperties.getTtl()).thenReturn(ttl);
        when(ttl.getFloodDetail()).thenReturn(3600L);
    }

    @Test
    void cacheActiveFlood_nullEvent_skips() {
        redisFloodGeoCache.cacheActiveFlood(null);
    }

    @Test
    void cacheActiveFlood_nullEventId_skips() {
        FloodLifecycleEvent event = FloodLifecycleEvent.builder()
                .eventId(null)
                .lat(10.5).lon(106.5)
                .build();
        redisFloodGeoCache.cacheActiveFlood(event);
    }

    @Test
    void cacheActiveFlood_nullLat_skips() {
        FloodLifecycleEvent event = FloodLifecycleEvent.builder()
                .eventId("event-1")
                .lat(null).lon(106.5)
                .build();
        redisFloodGeoCache.cacheActiveFlood(event);
    }

    @Test
    void cacheActiveFlood_nullLon_skips() {
        FloodLifecycleEvent event = FloodLifecycleEvent.builder()
                .eventId("event-1")
                .lat(10.5).lon(null)
                .build();
        redisFloodGeoCache.cacheActiveFlood(event);
    }

    @Test
    void cacheActiveFlood_success_allFieldsPresent() {
        // Covers buildHashFields: all optional fields non-null
        FloodLifecycleEvent event = FloodLifecycleEvent.builder()
                .eventId("event-1")
                .lat(10.5).lon(106.5)
                .severityLevel("HIGH")
                .waterLevel(1.5)
                .location("Quận 12")
                .build();
        redisFloodGeoCache.cacheActiveFlood(event);
    }

    @Test
    void cacheActiveFlood_success_nullOptionalFields() {
        // Covers buildHashFields: severityLevel/waterLevel/location == null → ""
        FloodLifecycleEvent event = FloodLifecycleEvent.builder()
                .eventId("event-2")
                .lat(10.5).lon(106.5)
                .severityLevel(null)
                .waterLevel(null)
                .location(null)
                .build();
        redisFloodGeoCache.cacheActiveFlood(event);
    }

    @Test
    void cacheActiveFlood_exceptionThrown_logsError() {
        FloodLifecycleEvent event = FloodLifecycleEvent.builder()
                .eventId("event-err")
                .lat(10.5).lon(106.5)
                .build();
        doThrow(new RuntimeException("Redis connection error"))
                .when(cacheService).geoAdd(anyString(), any(Point.class), anyString());

        redisFloodGeoCache.cacheActiveFlood(event);
    }

    // --- removeFloodCache ---

    @Test
    void removeFloodCache_nullEventId_skips() {
        redisFloodGeoCache.removeFloodCache(null);
    }

    @Test
    void removeFloodCache_blankEventId_skips() {
        redisFloodGeoCache.removeFloodCache("   ");
    }

    @Test
    void removeFloodCache_success() {
        redisFloodGeoCache.removeFloodCache("event-1");
    }

    @Test
    void removeFloodCache_exceptionThrown_logsError() {
        doThrow(new RuntimeException("Redis error"))
                .when(cacheService).geoRemove(anyString(), anyString());
        redisFloodGeoCache.removeFloodCache("event-err");
    }

    // --- findFloodsInRadius ---

    @Test
    void findFloodsInRadius_geoResultsNull_returnsEmpty() {
        when(cacheService.geoRadius(anyString(), any(Circle.class))).thenReturn(null);
        redisFloodGeoCache.findFloodsInRadius(10.5, 106.5, 5.0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findFloodsInRadius_geoResultsEmpty_returnsEmpty() {
        GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults = mock(GeoResults.class);
        when(geoResults.getContent()).thenReturn(List.of());
        when(cacheService.geoRadius(anyString(), any(Circle.class))).thenReturn(geoResults);

        redisFloodGeoCache.findFloodsInRadius(10.5, 106.5, 5.0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findFloodsInRadius_rawMapNull_skipsEntry() {
        GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults = mock(GeoResults.class);
        GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult = mock(GeoResult.class);
        RedisGeoCommands.GeoLocation<Object> location = mock(RedisGeoCommands.GeoLocation.class);

        when(location.getName()).thenReturn("event-1");
        when(geoResult.getContent()).thenReturn(location);
        when(geoResults.getContent()).thenReturn(List.of(geoResult));
        when(cacheService.geoRadius(anyString(), any(Circle.class))).thenReturn(geoResults);
        when(cacheService.hGetAll(anyString())).thenReturn(null);

        redisFloodGeoCache.findFloodsInRadius(10.5, 106.5, 5.0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findFloodsInRadius_rawMapEmpty_skipsEntry() {
        GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults = mock(GeoResults.class);
        GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult = mock(GeoResult.class);
        RedisGeoCommands.GeoLocation<Object> location = mock(RedisGeoCommands.GeoLocation.class);

        when(location.getName()).thenReturn("event-1");
        when(geoResult.getContent()).thenReturn(location);
        when(geoResults.getContent()).thenReturn(List.of(geoResult));
        when(cacheService.geoRadius(anyString(), any(Circle.class))).thenReturn(geoResults);
        when(cacheService.hGetAll(anyString())).thenReturn(Map.of());

        redisFloodGeoCache.findFloodsInRadius(10.5, 106.5, 5.0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findFloodsInRadius_success_validMap() {
        // Covers mapToResponse + getString (val != null) + parseDouble (valid)
        GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults = mock(GeoResults.class);
        GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult = mock(GeoResult.class);
        RedisGeoCommands.GeoLocation<Object> location = mock(RedisGeoCommands.GeoLocation.class);

        when(location.getName()).thenReturn("event-1");
        when(geoResult.getContent()).thenReturn(location);
        when(geoResults.getContent()).thenReturn(List.of(geoResult));
        when(cacheService.geoRadius(anyString(), any(Circle.class))).thenReturn(geoResults);

        Map<Object, Object> rawMap = new HashMap<>();
        rawMap.put("eventId", "event-1");
        rawMap.put("lat", "10.5");
        rawMap.put("lon", "106.5");
        rawMap.put("severityLevel", "HIGH");
        rawMap.put("waterLevel", "1.5");
        rawMap.put("location", "Quận 12");
        rawMap.put("updatedAt", "2024-01-01T00:00:00");
        when(cacheService.hGetAll(anyString())).thenReturn(rawMap);

        redisFloodGeoCache.findFloodsInRadius(10.5, 106.5, 5.0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findFloodsInRadius_blankDoubleValue_parsesAsNull() {
        GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults = mock(GeoResults.class);
        GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult = mock(GeoResult.class);
        RedisGeoCommands.GeoLocation<Object> location = mock(RedisGeoCommands.GeoLocation.class);

        when(location.getName()).thenReturn("event-1");
        when(geoResult.getContent()).thenReturn(location);
        when(geoResults.getContent()).thenReturn(List.of(geoResult));
        when(cacheService.geoRadius(anyString(), any(Circle.class))).thenReturn(geoResults);

        Map<Object, Object> rawMap = new HashMap<>();
        rawMap.put("eventId", "event-1");
        rawMap.put("lat", "");
        rawMap.put("lon", " ");
        when(cacheService.hGetAll(anyString())).thenReturn(rawMap);

        redisFloodGeoCache.findFloodsInRadius(10.5, 106.5, 5.0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findFloodsInRadius_invalidDoubleValue_parsesAsNull() {
        GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults = mock(GeoResults.class);
        GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult = mock(GeoResult.class);
        RedisGeoCommands.GeoLocation<Object> location = mock(RedisGeoCommands.GeoLocation.class);

        when(location.getName()).thenReturn("event-1");
        when(geoResult.getContent()).thenReturn(location);
        when(geoResults.getContent()).thenReturn(List.of(geoResult));
        when(cacheService.geoRadius(anyString(), any(Circle.class))).thenReturn(geoResults);

        Map<Object, Object> rawMap = new HashMap<>();
        rawMap.put("eventId", "event-1");
        rawMap.put("lat", "not-a-number");
        rawMap.put("lon", "INVALID");
        rawMap.put("waterLevel", "abc");
        when(cacheService.hGetAll(anyString())).thenReturn(rawMap);

        redisFloodGeoCache.findFloodsInRadius(10.5, 106.5, 5.0);
    }

    @Test
    void findFloodsInRadius_exceptionThrown_returnsEmpty() {
        doThrow(new RuntimeException("Redis error"))
                .when(cacheService).geoRadius(anyString(), any(Circle.class));

        redisFloodGeoCache.findFloodsInRadius(10.5, 106.5, 5.0);
    }

    @Test
    void clearGeoIndex_callsDelete() {
        redisFloodGeoCache.clearGeoIndex();
    }
}
