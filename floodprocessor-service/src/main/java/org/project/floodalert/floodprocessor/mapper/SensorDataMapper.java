package org.project.floodalert.floodprocessor.mapper;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.response.EnrichedSensorData;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.springframework.stereotype.Component;

@Component
public class SensorDataMapper {
    public ProcessedSensorData mapToProcessedData(EnrichedSensorData enrichedData, FloodStatus status) {


        return ProcessedSensorData.builder()
                // Các trường từ EnrichedSensorData
                .sensorId(enrichedData.getSensorId())
                .waterLevel(enrichedData.getWaterLevel())
                .lat(enrichedData.getLat())
                .lon(enrichedData.getLon())
                .battery(enrichedData.getBattery())
                .timestamp(enrichedData.getTimestamp())
                .warningThreshold(enrichedData.getWarningThreshold())
                .dangerThreshold(enrichedData.getDangerThreshold())
                .locationName(enrichedData.getLocationName())
                .status(status)
                .build();
    }
}
