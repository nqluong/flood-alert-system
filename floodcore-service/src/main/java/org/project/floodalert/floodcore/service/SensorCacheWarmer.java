package org.project.floodalert.floodcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorCacheWarmer implements CommandLineRunner {
    private final SensorRepository sensorRepository;
    private final SensorSyncService redisSensorSyncService;

    @Override
    public void run(String... args) throws Exception {
        log.info("[CACHE WARMING] Bắt đầu khởi tạo cache cho sensors...");
        Instant startTime = Instant.now();

        try {
            // Lấy toàn bộ sensors từ database
            List<Sensor> sensors = sensorRepository.findAll();

            if (sensors.isEmpty()) {
                log.warn("[CACHE WARMING] Không tìm thấy sensor nào trong database");
                return;
            }

            log.info("[CACHE WARMING] Tìm thấy {} sensors cần đồng bộ lên Redis", sensors.size());

            // Đồng bộ lên Redis
            redisSensorSyncService.syncSensorsToCache(sensors);

            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);

            log.info("[CACHE WARMING] Đồng bộ thành công {} sensors trong {}ms",
                    sensors.size(), duration.toMillis());

        } catch (Exception e) {
            log.error("[CACHE WARMING] Lỗi khi khởi tạo cache: {}", e.getMessage(), e);
            throw e;
        }
    }

}

