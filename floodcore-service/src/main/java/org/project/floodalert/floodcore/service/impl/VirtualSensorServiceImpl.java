package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.config.RedisKeyProperties;
import org.project.floodalert.floodcore.dto.request.VirtualSensorProvisionRequest;
import org.project.floodalert.floodcore.dto.response.VirtualSensorCleanupResponse;
import org.project.floodalert.floodcore.dto.response.VirtualSensorProvisionResponse;
import org.project.floodalert.floodcore.dto.response.VirtualSensorResponse;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.project.floodalert.floodcore.service.CacheService;
import org.project.floodalert.floodcore.service.VirtualSensorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualSensorServiceImpl implements VirtualSensorService {

    private final SensorRepository sensorRepository;
    private final CacheService cacheService;
    private final RedisKeyProperties redisKeyProperties;

    private static final double LAT_MIN = 20.9;
    private static final double LAT_MAX = 21.1;
    private static final double LON_MIN = 105.7;
    private static final double LON_MAX = 105.9;

    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("40.0");
    private static final BigDecimal DANGER_THRESHOLD = new BigDecimal("80.0");

    @Override
    @Transactional
    public VirtualSensorProvisionResponse provisionVirtualSensors(VirtualSensorProvisionRequest request) {
        Integer targetCount = request.getTargetCount();
        
        log.info("Bắt đầu provisioning virtual sensors. Target count: {}", targetCount);

        long currentCount = sensorRepository.countByIsVirtual(true);
        log.info("Số lượng virtual sensors hiện có: {}", currentCount);

        int createdCount = 0;

        if (currentCount < targetCount) {
            int needToCreate = (int) (targetCount - currentCount);
            log.info("Cần tạo thêm {} virtual sensors", needToCreate);
            int startSequence = getNextVirtualSequence();

            final int BATCH_SIZE = 500;
            int totalBatches = (int) Math.ceil((double) needToCreate / BATCH_SIZE);
            
            log.info("Chia thành {} batches, mỗi batch tối đa {} sensors", totalBatches, BATCH_SIZE);

            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                int batchStart = batchIndex * BATCH_SIZE;
                int batchEnd = Math.min(batchStart + BATCH_SIZE, needToCreate);
                int batchSize = batchEnd - batchStart;
                
                log.info("Xử lý batch {}/{}: từ {} đến {} ({} sensors)", 
                        batchIndex + 1, totalBatches, batchStart, batchEnd - 1, batchSize);

                // Tạo sensors cho batch này
                List<Sensor> batchSensors = createBatchSensors(startSequence + batchStart, batchSize);
                
                // Lưu batch vào DB
                List<Sensor> savedSensors = sensorRepository.saveAll(batchSensors);
                log.info("Đã lưu batch {}/{} vào PostgreSQL: {} sensors", 
                        batchIndex + 1, totalBatches, savedSensors.size());

                // Đồng bộ batch vào Redis
                syncSensorsToRedis(savedSensors);
                log.info("Đã đồng bộ batch {}/{} vào Redis", batchIndex + 1, totalBatches);

                createdCount += savedSensors.size();
                
                batchSensors.clear();
                savedSensors.clear();
            }

            log.info("Hoàn thành provisioning: đã tạo {} virtual sensors", createdCount);
        } else {
            log.info("Đã đủ số lượng virtual sensors ({}), không cần tạo thêm", currentCount);
        }

        List<Sensor> allVirtualSensors = sensorRepository.findByIsVirtual(true);
        List<VirtualSensorResponse> sensorResponses = allVirtualSensors.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        String message = createdCount > 0 
                ? String.format("Đã tạo thêm %d virtual sensors. Tổng cộng: %d sensors", 
                        createdCount, allVirtualSensors.size())
                : String.format("Đã có đủ %d virtual sensors, không cần tạo thêm", 
                        allVirtualSensors.size());

        return VirtualSensorProvisionResponse.builder()
                .totalCount(allVirtualSensors.size())
                .createdCount(createdCount)
                .existingCount((int) currentCount)
                .sensors(sensorResponses)
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public VirtualSensorCleanupResponse cleanupVirtualSensors() {
        log.warn("Bắt đầu cleanup toàn bộ virtual sensors");

        // Lấy danh sách virtual sensors
        List<Sensor> virtualSensors = sensorRepository.findByIsVirtual(true);
        int count = virtualSensors.size();

        if (count == 0) {
            log.info("Không có virtual sensors nào để xóa");
            return VirtualSensorCleanupResponse.builder()
                    .deletedCount(0)
                    .message("Không có virtual sensors nào trong hệ thống")
                    .build();
        }

        log.info("Tìm thấy {} virtual sensors cần xóa", count);

        // Xóa khỏi Redis trước (để Ingestion Service không nhận data nữa)
        List<String> sensorIds = virtualSensors.stream()
                .map(Sensor::getSensorId)
                .collect(Collectors.toList());
        
        removeSensorsFromRedis(sensorIds);
        log.info("Đã xóa {} virtual sensors khỏi Redis", count);

        // Xóa khỏi PostgreSQL
        sensorRepository.deleteAll(virtualSensors);
        log.warn("Đã xóa {} virtual sensors khỏi PostgreSQL", count);

        String message = String.format("Đã xóa thành công %d virtual sensors khỏi hệ thống", count);

        return VirtualSensorCleanupResponse.builder()
                .deletedCount(count)
                .message(message)
                .build();
    }

    private List<Sensor> createBatchSensors(int startSequence, int count) {
        LocalDateTime now = LocalDateTime.now();
        
        return java.util.stream.IntStream.range(0, count)
                .parallel()
                .mapToObj(i -> {
                    int sequence = startSequence + i;
                    String sensorId = String.format("VIRTUAL_%04d", sequence);
                    String apiKey = UUID.randomUUID().toString();
                    
                    BigDecimal lat = randomBigDecimal(LAT_MIN, LAT_MAX, 8);
                    BigDecimal lon = randomBigDecimal(LON_MIN, LON_MAX, 8);

                    int batteryLevel = ThreadLocalRandom.current().nextInt(70, 101);
                    int signalStrength = ThreadLocalRandom.current().nextInt(-80, -40);

                    return Sensor.builder()
                            .sensorId(sensorId)
                            .name("Virtual Sensor " + sequence)
                            .locationName("Hanoi Virtual Location " + sequence)
                            .lat(lat)
                            .lon(lon)
                            .status("ACTIVE")
                            .apiKey(apiKey)
                            .warningThreshold(WARNING_THRESHOLD)
                            .dangerThreshold(DANGER_THRESHOLD)
                            .hardwareModel("VIRTUAL-MODEL-V1")
                            .firmwareVersion("1.0.0-virtual")
                            .batteryLevel(batteryLevel)
                            .signalStrength(signalStrength)
                            .installedAt(now)
                            .lastHeartbeat(now)
                            .isVirtual(true)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private int getNextVirtualSequence() {
        List<String> existingIds = sensorRepository.findAllVirtualSensorIds();
        
        if (existingIds.isEmpty()) {
            return 1;
        }

        int maxSequence = existingIds.stream()
                .map(id -> id.replace("VIRTUAL_", ""))
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        return maxSequence + 1;
    }

    private void syncSensorsToRedis(List<Sensor> sensors) {
        try {
            Map<String, Map<String, String>> batchData = sensors.parallelStream()
                    .collect(Collectors.toConcurrentMap(
                            sensor -> redisKeyProperties.getKeys().getSensor().getInfoKey(sensor.getSensorId()),
                            sensor -> {
                                Map<String, String> fields = new HashMap<>();
                                fields.put("id", sensor.getId().toString());
                                fields.put("sensorId", sensor.getSensorId());
                                fields.put("api_key", sensor.getApiKey());
                                fields.put("status", sensor.getStatus());
                                fields.put("is_virtual", "true");
                                fields.put("warning_threshold", sensor.getWarningThreshold().toString());
                                fields.put("danger_threshold", sensor.getDangerThreshold().toString());
                                fields.put("lat", sensor.getLat().toString());
                                fields.put("lon", sensor.getLon().toString());
                                return fields;
                            }
                    ));

            cacheService.batchHSetFullKeys(batchData);
            
        } catch (Exception e) {
            log.error("Lỗi khi sync batch sensors vào Redis: {}", e.getMessage(), e);

        }
    }


    private void removeSensorsFromRedis(List<String> sensorIds) {
        List<String> keysToDelete = sensorIds.stream()
                .map(sensorId -> redisKeyProperties.getKeys().getSensor().getInfoKey(sensorId))
                .collect(Collectors.toList());

        try {
            cacheService.batchDelete(keysToDelete);
            log.info("Đã xóa {} keys khỏi Redis", keysToDelete.size());
        } catch (Exception e) {
            log.error("Lỗi khi xóa batch keys khỏi Redis: {}", e.getMessage(), e);
        }
    }

    private VirtualSensorResponse toResponse(Sensor sensor) {
        return VirtualSensorResponse.builder()
                .sensorId(sensor.getSensorId())
                .apiKey(sensor.getApiKey())
                .lat(sensor.getLat())
                .lon(sensor.getLon())
                .name(sensor.getName())
                .status(sensor.getStatus())
                .build();
    }


    private BigDecimal randomBigDecimal(double min, double max, int scale) {
        double random = ThreadLocalRandom.current().nextDouble(min, max);
        return BigDecimal.valueOf(random).setScale(scale, RoundingMode.HALF_UP);
    }
}
