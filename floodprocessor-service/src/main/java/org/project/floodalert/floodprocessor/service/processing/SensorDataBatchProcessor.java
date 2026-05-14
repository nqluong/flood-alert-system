package org.project.floodalert.floodprocessor.service.processing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.SensorMessage;
import org.project.floodalert.floodprocessor.dto.response.EnrichedSensorData;
import org.project.floodalert.floodprocessor.service.cache.RedisCacheService;
import org.project.floodalert.floodprocessor.service.core.BusinessLogicService;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataBatchProcessor {

    private final RedisCacheService redisCacheService;
    private final BusinessLogicService businessLogicService;

    /**
     * Xử lý batch sensor messages
     * 
     * @param messages Danh sách sensor messages từ Kafka (đã validated)
     */
    public void processBatch(List<SensorMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("Batch messages rỗng, bỏ qua");
            return;
        }

        try {
            Set<String> sensorIds = extractSensorIds(messages);

            Map<String, Map<String, String>> redisDataMap = redisCacheService.bulkFetchSensorInfo(sensorIds);

            // Enrich & Validate - kết hợp Kafka data với Redis data
            List<EnrichedSensorData> enrichedDataList = enrichAndValidate(
                    messages,
                    redisDataMap);

            if (enrichedDataList.isEmpty()) {
                log.warn("Không có enriched data hợp lệ sau khi enrich & validate");
                return;
            }

            // Step 4: Handover to Business Logic
            businessLogicService.process(enrichedDataList);

            log.info("Xử lý batch thành công: {}/{} messages hợp lệ",
                    enrichedDataList.size(), messages.size());

        } catch (Exception e) {
            Throwable rootCause = e.getCause() != null ? e.getCause() : e;
            log.error("Lỗi nghiêm trọng khi xử lý batch {} messages. Root cause: {}",
                    messages.size(), rootCause.getMessage(), e);
            throw new RuntimeException("Failed to process sensor batch", e);
        }
    }

    private Set<String> extractSensorIds(List<SensorMessage> messages) {
        Set<String> sensorIds = new HashSet<>();
        for (SensorMessage message : messages) {
            String sensorId = message.getSensorId();

            if (sensorId == null || sensorId.isEmpty()) {
                if (message.getSensorData() != null &&
                        message.getSensorData().getDeviceInfo() != null) {
                    sensorId = message.getSensorData().getDeviceInfo().getSensorId();
                }
            }

            if (sensorId != null && !sensorId.isEmpty()) {
                sensorIds.add(sensorId);
            }
        }
        return sensorIds;
    }

    /**
     * Enrich Kafka data với Redis metadata và validate
     * 
     * Note: Ingestion service đã validate:
     * - Timestamp, Blacklist, API key, Status, Water level sanity
     * FloodProcessor chỉ cần validate:
     * - Sensor registration (để lấy metadata)
     * - Required thresholds (warning_threshold, danger_threshold)
     */
    private List<EnrichedSensorData> enrichAndValidate(
            List<SensorMessage> messages,
            Map<String, Map<String, String>> redisDataMap) {

        List<EnrichedSensorData> enrichedList = new ArrayList<>();

        for (SensorMessage message : messages) {
            try {
                // Defensive null checks (nên giữ lại)
                if (message.getSensorData() == null) {
                    log.warn("Message có sensorData = null, bỏ qua. Message: {}", message);
                    continue;
                }

                var sensorData = message.getSensorData();

                if (sensorData.getDeviceInfo() == null || sensorData.getTelemetry() == null) {
                    log.warn("Message thiếu deviceInfo hoặc telemetry, bỏ qua. SensorData: {}", sensorData);
                    continue;
                }

                var deviceInfo = sensorData.getDeviceInfo();
                var telemetry = sensorData.getTelemetry();
                var health = sensorData.getHealth();

                String sensorId = message.getSensorId();
                if (sensorId == null || sensorId.isEmpty()) {
                    sensorId = deviceInfo.getSensorId();
                }

                if (sensorId == null || sensorId.isEmpty()) {
                    log.warn("Không tìm thấy sensorId trong message, bỏ qua. Message: {}", message);
                    continue;
                }

                // Lấy metadata từ Redis
                Map<String, String> redisData = redisDataMap.get(sensorId);

                // Kiểm tra sensor đã đăng ký (cần metadata để enrich)
                if (redisData == null || redisData.isEmpty()) {
                    log.warn("Sensor {} chưa được đăng ký trong Redis, DROP message", sensorId);
                    continue;
                }

                // Parse thresholds từ Redis
                Double warningLevel = parseDoubleFromRedis(redisData.get("warning_threshold"));
                Double dangerLevel = parseDoubleFromRedis(redisData.get("danger_threshold"));
                String streetName = redisData.get("location_name");
                String district = redisData.get("district");

                // Validate required thresholds
                if (warningLevel == null || dangerLevel == null) {
                    log.warn("Sensor {} thiếu warning_threshold hoặc danger_threshold trong Redis, DROP message",
                            sensorId);
                    continue;
                }

                String locationName = buildLocationName(streetName, district);

                // Build EnrichedSensorData
                EnrichedSensorData enriched = EnrichedSensorData.builder()
                        .sensorId(sensorId)
                        .firmwareVer(deviceInfo.getFirmwareVer() != null ? deviceInfo.getFirmwareVer() : "Unknown")
                        .model(deviceInfo.getModel() != null ? deviceInfo.getModel() : "Unknown")
                        .waterLevel(telemetry.getWaterLevel())
                        .lat(telemetry.getLat())
                        .lon(telemetry.getLon())
                        .battery(health != null ? health.getBatteryLevel() : null)
                        .timestamp(sensorData.getTimestamp())
                        .warningThreshold(warningLevel)
                        .dangerThreshold(dangerLevel)
                        .locationName(locationName)
                        .temperature(health != null ? health.getTemperature() : null)
                        .signalStrength(health != null ? health.getSignalStrength() : null)
                        .deviceStatus(health != null && health.getStatus() != null ? health.getStatus() : "UNKNOWN")
                        .build();

                enrichedList.add(enriched);

            } catch (Exception e) {
                log.error("Lỗi khi enrich data cho message, bỏ qua. Error: {}, Message: {}",
                        e.getMessage(), message, e);
            }
        }

        log.info("Enrich & validate hoàn tất: {}/{} messages hợp lệ",
                enrichedList.size(), messages.size());

        return enrichedList;
    }

    private Double parseDoubleFromRedis(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("Không thể parse giá trị Double từ Redis: {}", value);
            return null;
        }
    }

    private String buildLocationName(String streetName, String district) {
        if (streetName != null && district != null) {
            return streetName + ", " + district;
        } else if (streetName != null) {
            return streetName;
        } else if (district != null) {
            return district;
        }
        return "Unknown Location";
    }
}
