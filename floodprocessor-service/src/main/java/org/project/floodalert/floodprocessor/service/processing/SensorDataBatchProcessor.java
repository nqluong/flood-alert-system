package org.project.floodalert.floodprocessor.service.processing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.SensorMessage;
import org.project.floodalert.floodprocessor.dto.response.EnrichedSensorData;
import org.project.floodalert.floodprocessor.service.cache.RedisCacheService;
import org.project.floodalert.floodprocessor.service.core.BusinessLogicService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service xử lý batch sensor data từ Kafka
 * Thực hiện: Extract → Enrich → Validate → Handover to Business Logic
 * 
 * Note: Data đã được parse & validate ở ingestion-service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataBatchProcessor {
    
    private final RedisCacheService redisCacheService;
    private final BusinessLogicService businessLogicService;

    /**
     * Xử lý batch sensor messages
     * @param messages Danh sách sensor messages từ Kafka (đã validated)
     */
    public void processBatch(List<SensorMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("Batch messages rỗng, bỏ qua");
            return;
        }

        try {
            // Extract sensor IDs (không cần parse nữa!)
            Set<String> sensorIds = extractSensorIds(messages);

            // Bulk Enrichment - lấy thông tin sensor từ Redis
            Map<String, Map<String, String>> redisDataMap = 
                    redisCacheService.bulkFetchSensorInfo(sensorIds);

            // Enrich & Validate - kết hợp Kafka data với Redis data
            List<EnrichedSensorData> enrichedDataList = enrichAndValidate(
                    messages,
                    redisDataMap
            );

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

    /**
     * Extract sensor IDs từ messages
     */
    private Set<String> extractSensorIds(List<SensorMessage> messages) {
        Set<String> sensorIds = new HashSet<>();
        for (SensorMessage message : messages) {
            if (message.getSensorData() != null && 
                message.getSensorData().getDeviceInfo() != null) {
                sensorIds.add(message.getSensorData().getDeviceInfo().getSensorId());
            }
        }
        return sensorIds;
    }

    /**
     * Enrich Kafka data với Redis metadata và validate
     */
    private List<EnrichedSensorData> enrichAndValidate(
            List<SensorMessage> messages,
            Map<String, Map<String, String>> redisDataMap) {

        List<EnrichedSensorData> enrichedList = new ArrayList<>();

        for (SensorMessage message : messages) {
            try {
                var sensorData = message.getSensorData();
                var deviceInfo = sensorData.getDeviceInfo();
                var telemetry = sensorData.getTelemetry();
                var health = sensorData.getHealth();
                
                String sensorId = deviceInfo.getSensorId();
                Map<String, String> redisData = redisDataMap.get(sensorId);

                // DROP message nếu sensor không được seed/register trong Redis
                if (redisData == null || redisData.isEmpty()) {
                    log.warn("Sensor {} chưa được đăng ký trong Redis, DROP message", sensorId);
                    continue;
                }

                // Parse Redis data
                Double warningLevel = parseDoubleFromRedis(redisData.get("warning_threshold"));
                Double dangerLevel = parseDoubleFromRedis(redisData.get("danger_threshold"));
                String streetName = redisData.get("location_name");
                String district = redisData.get("district");

                // Validate Redis data
                if (warningLevel == null || dangerLevel == null) {
                    log.warn("Sensor {} thiếu warning_level hoặc danger_level trong Redis, DROP message",
                            sensorId);
                    continue;
                }

                String locationName = buildLocationName(streetName, district);

                // Build EnrichedSensorData
                EnrichedSensorData enriched = EnrichedSensorData.builder()
                        .sensorId(sensorId)
                        .firmwareVer(deviceInfo.getFirmwareVer() != null ? 
                                deviceInfo.getFirmwareVer() : "Unknown")
                        .model(deviceInfo.getModel() != null ? 
                                deviceInfo.getModel() : "Unknown")
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
                        .deviceStatus(health != null && health.getStatus() != null ? 
                                health.getStatus() : "UNKNOWN")
                        .build();

                enrichedList.add(enriched);

            } catch (Exception e) {
                log.error("Lỗi khi enrich data cho message, bỏ qua: {}", e.getMessage());
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

    /**
     * Build location name từ street_name và district
     */
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
