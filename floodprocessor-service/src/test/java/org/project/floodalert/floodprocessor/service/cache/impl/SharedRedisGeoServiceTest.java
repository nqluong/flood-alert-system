package org.project.floodalert.floodprocessor.service.cache.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedRedisGeoServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private GeoOperations<String, Object> geoOperations;

    @InjectMocks
    private SharedRedisGeoService service;

    @Test
    void updateEventPosition_success() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        service.updateEventPosition("evt1", 10.0, 106.0);
    }

    @Test
    void updateEventPosition_exceptionThrown_logsError() {
        when(redisTemplate.opsForGeo()).thenThrow(new RuntimeException("Redis down"));
        service.updateEventPosition("evt1", 10.0, 106.0);
    }

    @Test
    void findNearbyActiveFlood_resultsNull_returnsEmpty() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(anyString(), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(null);

        service.findNearbyActiveFlood(10.0, 106.0, 5.0);
    }

    @Test
    void findNearbyActiveFlood_emptyResults_returnsEmpty() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        GeoResults<RedisGeoCommands.GeoLocation<Object>> emptyResults =
                new GeoResults<>(Collections.emptyList());
        when(geoOperations.radius(anyString(), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(emptyResults);

        service.findNearbyActiveFlood(10.0, 106.0, 5.0);
    }

    @Test
    void findNearbyActiveFlood_hasResult_returnsEventId() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);

        RedisGeoCommands.GeoLocation<Object> location =
                new RedisGeoCommands.GeoLocation<>("evt-123", null);
        GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult =
                new GeoResult<>(location, new Distance(1.5));
        GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                new GeoResults<>(List.of(geoResult));

        when(geoOperations.radius(anyString(), any(Circle.class), any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(results);

        service.findNearbyActiveFlood(10.0, 106.0, 5.0);
    }

    @Test
    void findNearbyActiveFlood_exceptionThrown_returnsEmpty() {
        when(redisTemplate.opsForGeo()).thenThrow(new RuntimeException("Geo error"));

        service.findNearbyActiveFlood(10.0, 106.0, 5.0);
    }
}