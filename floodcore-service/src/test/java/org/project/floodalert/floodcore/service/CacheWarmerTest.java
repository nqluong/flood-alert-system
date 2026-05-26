package org.project.floodalert.floodcore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodcore.model.CoreActiveFlood;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.repository.CoreActiveFloodRepository;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.springframework.boot.ApplicationArguments;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheWarmerTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private SensorSyncService sensorSyncService;

    @Mock
    private CoreActiveFloodRepository coreActiveFloodRepository;

    @Mock
    private FloodGeoCache floodGeoCache;

    @Mock
    private ApplicationArguments args;

    @InjectMocks
    private CacheWarmer cacheWarmer;

    private Sensor sensor;
    private CoreActiveFlood flood;

    @BeforeEach
    void setUp() {
        sensor = new Sensor();

        flood = CoreActiveFlood.builder()
                .eventId("flood-001")
                .lat(BigDecimal.valueOf(21.01))
                .lon(BigDecimal.valueOf(105.85))
                .locationDescription("Ha Noi")
                .waterLevel(BigDecimal.valueOf(0.8))
                .severityLevel("MEDIUM")
                .build();
    }

    @Test
    void run_hasSensorsAndFloods_syncsSensorsAndCachesFloods() {
        when(sensorRepository.findAll()).thenReturn(List.of(sensor));
        when(coreActiveFloodRepository.findAll()).thenReturn(List.of(flood));

        cacheWarmer.run(args);

        verify(sensorRepository).findAll();
        verify(sensorSyncService).syncSensorsToCache(List.of(sensor));

        verify(floodGeoCache).clearGeoIndex();
        verify(coreActiveFloodRepository).findAll();
        verify(floodGeoCache).cacheActiveFlood(any(FloodLifecycleEvent.class));
    }

    @Test
    void run_noSensors_noFloods_onlyClearsFloodGeoIndex() {
        when(sensorRepository.findAll()).thenReturn(List.of());
        when(coreActiveFloodRepository.findAll()).thenReturn(List.of());

        cacheWarmer.run(args);

        verify(sensorRepository).findAll();
        verify(sensorSyncService, never()).syncSensorsToCache(any());

        verify(floodGeoCache).clearGeoIndex();
        verify(coreActiveFloodRepository).findAll();
        verify(floodGeoCache, never()).cacheActiveFlood(any());
    }

    @Test
    void run_noSensors_hasFloods_cachesFloodsOnly() {
        when(sensorRepository.findAll()).thenReturn(List.of());
        when(coreActiveFloodRepository.findAll()).thenReturn(List.of(flood));

        cacheWarmer.run(args);

        verify(sensorRepository).findAll();
        verify(sensorSyncService, never()).syncSensorsToCache(any());

        verify(floodGeoCache).clearGeoIndex();
        verify(coreActiveFloodRepository).findAll();
        verify(floodGeoCache).cacheActiveFlood(any(FloodLifecycleEvent.class));
    }

    @Test
    void run_hasSensors_noFloods_syncsSensorsOnly() {
        when(sensorRepository.findAll()).thenReturn(List.of(sensor));
        when(coreActiveFloodRepository.findAll()).thenReturn(List.of());

        cacheWarmer.run(args);

        verify(sensorRepository).findAll();
        verify(sensorSyncService).syncSensorsToCache(List.of(sensor));

        verify(floodGeoCache).clearGeoIndex();
        verify(coreActiveFloodRepository).findAll();
        verify(floodGeoCache, never()).cacheActiveFlood(any());
    }

    @Test
    void run_floodWithNullValues_stillCachesFlood() {
        CoreActiveFlood floodWithNullValues = CoreActiveFlood.builder()
                .eventId("flood-002")
                .lat(null)
                .lon(null)
                .locationDescription(null)
                .waterLevel(null)
                .severityLevel(null)
                .build();

        when(sensorRepository.findAll()).thenReturn(List.of());
        when(coreActiveFloodRepository.findAll()).thenReturn(List.of(floodWithNullValues));

        cacheWarmer.run(args);

        verify(floodGeoCache).clearGeoIndex();
        verify(coreActiveFloodRepository).findAll();
        verify(floodGeoCache).cacheActiveFlood(any(FloodLifecycleEvent.class));
    }

    @Test
    void run_oneFloodCacheThrows_continuesWithNextFlood() {
        CoreActiveFlood flood2 = CoreActiveFlood.builder()
                .eventId("flood-002")
                .lat(BigDecimal.valueOf(20.5))
                .lon(BigDecimal.valueOf(106.1))
                .locationDescription("Nam Dinh")
                .waterLevel(BigDecimal.valueOf(1.2))
                .severityLevel("MEDIUM")
                .build();

        when(sensorRepository.findAll()).thenReturn(List.of());
        when(coreActiveFloodRepository.findAll()).thenReturn(List.of(flood, flood2));

        doThrow(new RuntimeException("Redis error"))
                .doNothing()
                .when(floodGeoCache)
                .cacheActiveFlood(any(FloodLifecycleEvent.class));

        cacheWarmer.run(args);

        verify(floodGeoCache).clearGeoIndex();
        verify(floodGeoCache, times(2)).cacheActiveFlood(any(FloodLifecycleEvent.class));
    }

    @Test
    void run_sensorRepositoryThrows_stillRunsFloodTask() {
        when(sensorRepository.findAll()).thenThrow(new RuntimeException("DB sensor error"));
        when(coreActiveFloodRepository.findAll()).thenReturn(List.of(flood));

        cacheWarmer.run(args);

        verify(sensorRepository).findAll();

        verify(floodGeoCache).clearGeoIndex();
        verify(coreActiveFloodRepository).findAll();
        verify(floodGeoCache).cacheActiveFlood(any(FloodLifecycleEvent.class));
    }

    @Test
    void run_coreActiveFloodRepositoryThrows_stillRunsSensorTask() {
        when(sensorRepository.findAll()).thenReturn(List.of(sensor));
        when(coreActiveFloodRepository.findAll()).thenThrow(new RuntimeException("DB flood error"));

        cacheWarmer.run(args);

        verify(sensorRepository).findAll();
        verify(sensorSyncService).syncSensorsToCache(List.of(sensor));

        verify(floodGeoCache).clearGeoIndex();
        verify(coreActiveFloodRepository).findAll();
    }
}