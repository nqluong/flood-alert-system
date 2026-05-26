package org.project.floodalert.floodcore.service.saferoute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.config.RedisKeyProperties;
import org.project.floodalert.floodcore.service.CacheService;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloodSpatialQueryServiceTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private RedisKeyProperties redisKeyProperties;

    @Mock
    private RedisKeyProperties.Keys keys;

    @Mock
    private RedisKeyProperties.FloodKeys floodKeys;

    @InjectMocks
    private FloodSpatialQueryService service;

    @BeforeEach
    void setUp() {
        when(redisKeyProperties.getKeys()).thenReturn(keys);
        when(keys.getFlood()).thenReturn(floodKeys);
    }

    @Test
    void findFloodIdsInBoundingBox_geoResultsEmpty_returnsEmptyList() {
        GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults =
                new GeoResults<>(List.of());

        when(floodKeys.getGeoIndex()).thenReturn("flood:geo");
        when(cacheService.geoRadius(anyString(), any(Circle.class)))
                .thenReturn(geoResults);

        service.findFloodIdsInBoundingBox(new double[]{106.6, 10.7, 106.8, 10.9});

        verify(cacheService).geoRadius(anyString(), any(Circle.class));
    }

    @Test
    void findFloodIdsInBoundingBox_cacheServiceThrows_returnsEmptyList() {
        when(floodKeys.getGeoIndex()).thenReturn("flood:geo");
        when(cacheService.geoRadius(anyString(), any(Circle.class)))
                .thenThrow(new RuntimeException("Redis error"));

        service.findFloodIdsInBoundingBox(new double[]{106.6, 10.7, 106.8, 10.9});

        verify(cacheService).geoRadius(anyString(), any(Circle.class));
    }

    @Test
    void batchGetFloodDetails_pipelineResultsNull_returnsEmptyList() {
        when(floodKeys.getDetailKey("flood-001")).thenReturn("flood:detail:flood-001");
        when(cacheService.executePipeline(any()))
                .thenReturn(null);

        service.batchGetFloodDetails(List.of("flood-001"));

        verify(cacheService).executePipeline(any());
    }

    @Test
    void batchGetFloodDetails_pipelineResultsEmpty_returnsEmptyList() {
        when(floodKeys.getDetailKey("flood-001")).thenReturn("flood:detail:flood-001");
        when(cacheService.executePipeline(any()))
                .thenReturn(List.of());

        service.batchGetFloodDetails(List.of("flood-001"));

        verify(cacheService).executePipeline(any());
    }

    @Test
    void batchGetFloodDetails_validPipelineResult_returnsDetails() {
        Map<Object, Object> rawMap = Map.of(
                "eventId", "flood-001",
                "lat", "10.7",
                "lon", "106.7",
                "waterLevel", "1.2"
        );

        when(floodKeys.getDetailKey("flood-001")).thenReturn("flood:detail:flood-001");
        when(cacheService.executePipeline(any()))
                .thenReturn(List.of(rawMap));

        service.batchGetFloodDetails(List.of("flood-001"));

        verify(cacheService).executePipeline(any());
    }

    @Test
    void batchGetFloodDetails_invalidPipelineResult_skipsItem() {
        Map<Object, Object> rawMap = Map.of(
                "eventId", "flood-001",
                "lat", "invalid",
                "lon", "106.7",
                "waterLevel", "1.2"
        );

        when(floodKeys.getDetailKey("flood-001")).thenReturn("flood:detail:flood-001");
        when(cacheService.executePipeline(any()))
                .thenReturn(List.of(rawMap));

        service.batchGetFloodDetails(List.of("flood-001"));

        verify(cacheService).executePipeline(any());
    }

    @Test
    void batchGetFloodDetails_byteArrayPipelineResult_returnsDetails() {
        Map<Object, Object> rawMap = Map.of(
                "eventId".getBytes(StandardCharsets.UTF_8), "flood-001".getBytes(StandardCharsets.UTF_8),
                "lat".getBytes(StandardCharsets.UTF_8), "10.7".getBytes(StandardCharsets.UTF_8),
                "lon".getBytes(StandardCharsets.UTF_8), "106.7".getBytes(StandardCharsets.UTF_8),
                "waterLevel".getBytes(StandardCharsets.UTF_8), "1.2".getBytes(StandardCharsets.UTF_8)
        );

        when(floodKeys.getDetailKey("flood-001")).thenReturn("flood:detail:flood-001");
        when(cacheService.executePipeline(any()))
                .thenReturn(List.of(rawMap));

        service.batchGetFloodDetails(List.of("flood-001"));

        verify(cacheService).executePipeline(any());
    }

    @Test
    void batchGetFloodDetails_multipleIds_usesDetailKeysAndPipeline() {
        when(floodKeys.getDetailKey("flood-001")).thenReturn("flood:detail:flood-001");
        when(floodKeys.getDetailKey("flood-002")).thenReturn("flood:detail:flood-002");

        Map<Object, Object> rawMap1 = Map.of(
                "eventId", "flood-001",
                "lat", "10.7",
                "lon", "106.7",
                "waterLevel", "1.2"
        );

        Map<Object, Object> rawMap2 = Map.of(
                "eventId", "flood-002",
                "lat", "10.8",
                "lon", "106.8",
                "waterLevel", "0.9"
        );

        when(cacheService.executePipeline(any()))
                .thenReturn(List.of(rawMap1, rawMap2));

        service.batchGetFloodDetails(List.of("flood-001", "flood-002"));

        verify(floodKeys).getDetailKey("flood-001");
        verify(floodKeys).getDetailKey("flood-002");
        verify(cacheService).executePipeline(any());
    }

    @Test
    void batchGetFloodDetails_pipelineResultsMoreThanIds_usesMinSize() {
        when(floodKeys.getDetailKey("flood-001")).thenReturn("flood:detail:flood-001");

        Map<Object, Object> rawMap1 = Map.of(
                "eventId", "flood-001",
                "lat", "10.7",
                "lon", "106.7",
                "waterLevel", "1.2"
        );

        Map<Object, Object> rawMap2 = Map.of(
                "eventId", "flood-002",
                "lat", "10.8",
                "lon", "106.8",
                "waterLevel", "0.9"
        );

        when(cacheService.executePipeline(any()))
                .thenReturn(List.of(rawMap1, rawMap2));

        service.batchGetFloodDetails(List.of("flood-001"));

        verify(cacheService).executePipeline(any());
    }

    @Test
    void batchGetFloodDetails_executePipelineThrows_returnsEmptyList() {
        when(floodKeys.getDetailKey("flood-001")).thenReturn("flood:detail:flood-001");
        when(cacheService.executePipeline(any()))
                .thenThrow(new RuntimeException("Pipeline error"));

        service.batchGetFloodDetails(List.of("flood-001"));

        verify(cacheService).executePipeline(any());
    }
}