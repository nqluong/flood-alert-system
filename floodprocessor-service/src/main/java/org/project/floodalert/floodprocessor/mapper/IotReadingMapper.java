package org.project.floodalert.floodprocessor.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.model.IoTReading;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IotReadingMapper {
    private final ObjectMapper objectMapper;

    public IoTReading toEntity(ProcessedSensorData dto){
        return IoTReading.builder()
                // Mapping từ DTO
                .readingId(UUID.randomUUID().toString())
                .sensorId(dto.getSensorId())
                .waterLevel(convertToDecimal(dto.getWaterLevel()))
                .batteryLevel(convertToInteger(dto.getBattery()))
                .signalStrength(dto.getSignalStrength())
                .temperature(convertToDecimal(dto.getTemperature()))
                .humidity(convertToDecimal(dto.getHumidity()))
                .measuredAt(Instant.ofEpochMilli(dto.getTimestamp()))
                .rawPayload(parseRawPayload(dto.getRawPayload()))
                .status(dto.getStatus() != null ? dto.getStatus().name() : null)
                .floodEventId(null)
                .build();
    }

    public List<IoTReading> toEntities(List<ProcessedSensorData> dtoList) {

        return dtoList.stream()
                .map(this::toEntity)
                .toList();
    }

    private BigDecimal convertToDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private Integer convertToInteger(Double value) {
        return value != null ? value.intValue() : null;
    }

    private JsonNode parseRawPayload(String rawPayload) {
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            log.warn("Không thể parse rawPayload sang JsonNode, lưu dưới dạng text node", e);
            return objectMapper.getNodeFactory().textNode(rawPayload);
        }
    }
}
