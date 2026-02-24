package org.project.floodalert.floodprocessor.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.dto.request.SensorMessage;
import org.project.floodalert.floodprocessor.dto.request.SensorRaw;
import org.project.floodalert.floodprocessor.dto.response.EnrichedSensorData;
import org.project.floodalert.floodprocessor.exception.ProcessorErrorCode;
import org.project.floodalert.floodprocessor.service.BusinessLogicService;
import org.project.floodalert.floodprocessor.service.RedisCacheService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionBatchHandler {
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;
    private final BusinessLogicService businessLogicService;

    @KafkaListener(
            topics = "${app.kafka.topic.ingest}",
            groupId = "${app.kafka.consumer.group-id}",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeBatch(List<SensorMessage> messages, Acknowledgment acknowledgment) {
        log.info("=== BẮT ĐẦU XỬ LÝ BATCH: {} messages ===", messages.size());

        try {
            // Parse & Extract
            ParseResult parseResult = parseAndExtract(messages);

            if (parseResult.getValidDtos().isEmpty()) {
                log.warn("Không có message hợp lệ nào trong batch, bỏ qua");
                acknowledgment.acknowledge();
                return;
            }

            // Bulk Enrichment
            Map<String, Map<String, String>> redisDataMap = redisCacheService.bulkFetchSensorInfo(parseResult.getSensorIds());

            // Merge & Validate
            List<EnrichedSensorData> enrichedDataList = mergeAndValidate(
                    parseResult.getValidDtos(),
                    redisDataMap
            );

            if (enrichedDataList.isEmpty()) {
                acknowledgment.acknowledge();
                return;
            }

            // Handover to Module 2
            businessLogicService.process(enrichedDataList);

            log.info("=== HOÀN THÀNH XỬ LÝ BATCH: {} messages thành công, {} messages bị drop ===",
                    enrichedDataList.size(),
                    messages.size() - enrichedDataList.size());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi xử lý batch messages", e);
            throw new AppException(ProcessorErrorCode.PROCESSING_FAILED);
        }
    }


    private ParseResult parseAndExtract(List<SensorMessage> messages) {
        List<SensorRaw> validDtos = new ArrayList<>();
        Set<String> sensorIds = new HashSet<>();

        for (SensorMessage message : messages) {
            try {
                SensorRaw dto = objectMapper.readValue(message.getRawPayload(), SensorRaw.class);

                // Validate cơ bản
                if (dto.getDeviceInfo() == null || dto.getDeviceInfo().getSensorId() == null) {
                    log.warn("Message thiếu device_info hoặc sensor_id, bỏ qua");
                    continue;
                }

                if (dto.getTelemetry() == null || dto.getTelemetry().getWaterLevel() == null) {
                    log.warn("Message thiếu telemetry hoặc water_level cho sensor {}, bỏ qua",
                            dto.getDeviceInfo().getSensorId());
                    continue;
                }

                validDtos.add(dto);
                sensorIds.add(dto.getDeviceInfo().getSensorId());

            } catch (Exception e) {
                log.error("Lỗi parse JSON message, bỏ qua: {}", e.getMessage());
            }
        }

        log.info("Parse thành công {}/{} messages, thu thập {} sensor IDs duy nhất",
                validDtos.size(), messages.size(), sensorIds.size());

        return new ParseResult(validDtos, sensorIds);
    }

    /**
     * Merge Kafka data với Redis data và validate
     */
    private List<EnrichedSensorData> mergeAndValidate(
            List<SensorRaw> validDtos,
            Map<String, Map<String, String>> redisDataMap) {

        List<EnrichedSensorData> enrichedList = new ArrayList<>();

        for (SensorRaw dto : validDtos) {
            String sensorId = dto.getDeviceInfo().getSensorId();
            Map<String, String> redisData = redisDataMap.get(sensorId);

            // DROP message nếu sensor không được seed/register trong Redis
            if (redisData == null || redisData.isEmpty()) {
                log.warn("Sensor {} chưa được đăng ký trong Redis, DROP message", sensorId);
                continue;
            }

            try {
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
                        .waterLevel(dto.getTelemetry().getWaterLevel())
                        .lat(dto.getTelemetry().getLat())
                        .lon(dto.getTelemetry().getLon())
                        .battery(dto.getHealth() != null ? dto.getHealth().getBatteryLevel() : null)
                        .timestamp(dto.getTimestamp())
                        .warningThreshold(warningLevel)
                        .dangerThreshold(dangerLevel)
                        .locationName(locationName)
                        .build();

                enrichedList.add(enriched);

            } catch (Exception e) {
                log.error("Lỗi khi merge data cho sensor {}, bỏ qua: {}", sensorId, e.getMessage());
            }
        }

        log.info("Merge & validate hoàn tất: {}/{} messages hợp lệ",
                enrichedList.size(), validDtos.size());

        return enrichedList;
    }

    /**
     * Helper: Parse Double từ Redis String
     */
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

    private static class ParseResult {
        private final List<SensorRaw> validDtos;
        private final Set<String> sensorIds;

        public ParseResult(List<SensorRaw> validDtos, Set<String> sensorIds) {
            this.validDtos = validDtos;
            this.sensorIds = sensorIds;
        }

        public List<SensorRaw> getValidDtos() {
            return validDtos;
        }

        public Set<String> getSensorIds() {
            return sensorIds;
        }
    }
}
