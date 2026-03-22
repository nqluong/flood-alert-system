package org.project.floodalert.floodcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodcore.model.CoreActiveFlood;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.repository.CoreActiveFloodRepository;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheWarmer implements ApplicationRunner {

    private final SensorRepository          sensorRepository;
    private final SensorSyncService         sensorSyncService;
    private final CoreActiveFloodRepository coreActiveFloodRepository;
    private final FloodGeoCache             floodGeoCache;

    @Override
    public void run(ApplicationArguments args) {

        CompletableFuture<Void> sensorTask = CompletableFuture
                .runAsync(this::warmSensorCache)
                .exceptionally(ex -> {
                    log.error("[CacheWarmer] Sensor warm-up thất bại: {}", ex.getMessage(), ex);
                    return null; // tiếp tục – không chặn flood task
                });

        CompletableFuture<Void> floodTask = CompletableFuture
                .runAsync(this::warmFloodCache)
                .exceptionally(ex -> {
                    log.error("[CacheWarmer] Flood warm-up thất bại: {}", ex.getMessage(), ex);
                    return null;
                });

        // Chờ cả 2 task hoàn thành trước khi server nhận traffic
        CompletableFuture.allOf(sensorTask, floodTask).join();
    }

    private void warmSensorCache() {

        List<Sensor> sensors = sensorRepository.findAll();

        if (sensors.isEmpty()) {
            log.warn("[CacheWarmer][Sensor] Không tìm thấy sensor nào trong database");
            return;
        }

        sensorSyncService.syncSensorsToCache(sensors);
    }

    private void warmFloodCache() {
        // Xóa GEO index cũ để tránh dữ liệu orphan/stale
        floodGeoCache.clearGeoIndex();

        List<CoreActiveFlood> activeFloods = coreActiveFloodRepository.findAll();

        if (activeFloods.isEmpty()) {
            log.info("[CacheWarmer][Flood] Không có điểm ngập active, bỏ qua");
            return;
        }

        for (CoreActiveFlood flood : activeFloods) {
            try {
                floodGeoCache.cacheActiveFlood(toEvent(flood));
            } catch (Exception e) {
                log.warn("[CacheWarmer][Flood] Bỏ qua eventId={} do lỗi: {}",
                        flood.getEventId(), e.getMessage());
            }
        }
    }

    private FloodLifecycleEvent toEvent(CoreActiveFlood flood) {
        return FloodLifecycleEvent.builder()
                .eventId(flood.getEventId())
                .lat(flood.getLat() != null ? flood.getLat().doubleValue() : null)
                .lon(flood.getLon() != null ? flood.getLon().doubleValue() : null)
                .location(flood.getLocationDescription())
                .waterLevel(flood.getWaterLevel() != null ? flood.getWaterLevel().doubleValue() : null)
                .severityLevel(flood.getSeverityLevel())
                .build();
    }
}

