package org.project.floodalert.floodcore.mapper;

import org.project.floodalert.floodcore.dto.response.SensorDetailResponse;
import org.project.floodalert.floodcore.dto.response.SensorLogResponse;
import org.project.floodalert.floodcore.dto.response.SensorMapResponse;
import org.project.floodalert.floodcore.dto.response.SensorSummaryResponse;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.model.SensorLog;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SensorReadMapper {
    /**
     * Map Sensor entity sang SensorSummaryResponse (cho danh sách)
     */
    public SensorSummaryResponse toSummaryResponse(Sensor sensor) {
        return SensorSummaryResponse.builder()
                .id(sensor.getId())
                .sensorId(sensor.getSensorId())
                .name(sensor.getName())
                .locationName(sensor.getLocationName())
                .lat(sensor.getLat())
                .lon(sensor.getLon())
                .dangerThreshold(sensor.getDangerThreshold())
                .warningThreshold(sensor.getWarningThreshold())
                .status(sensor.getStatus())
                .batteryLevel(sensor.getBatteryLevel())
                .signalStrength(sensor.getSignalStrength())
                .lastHeartbeat(sensor.getLastHeartbeat())
                .lastReadingAt(sensor.getLastReadingAt())
                .hardwareModel(sensor.getHardwareModel())
                .firmwareVersion(sensor.getFirmwareVersion())
                .createdAt(sensor.getCreatedAt())
                .updatedAt(sensor.getUpdatedAt())
                .build();
    }

    /**
     * Map Sensor entity sang SensorDetailResponse (chi tiết đầy đủ)
     */
    public SensorDetailResponse toDetailResponse(Sensor sensor) {
        return SensorDetailResponse.builder()
                .id(sensor.getId())
                .sensorId(sensor.getSensorId())
                .name(sensor.getName())
                .locationName(sensor.getLocationName())
                .lat(sensor.getLat())
                .lon(sensor.getLon())
                .status(sensor.getStatus())
                .warningThreshold(sensor.getWarningThreshold())
                .dangerThreshold(sensor.getDangerThreshold())
                .hardwareModel(sensor.getHardwareModel())
                .firmwareVersion(sensor.getFirmwareVersion())
                .batteryLevel(sensor.getBatteryLevel())
                .signalStrength(sensor.getSignalStrength())
                .installedAt(sensor.getInstalledAt())
                .lastHeartbeat(sensor.getLastHeartbeat())
                .lastReadingAt(sensor.getLastReadingAt())
                .createdAt(sensor.getCreatedAt())
                .updatedAt(sensor.getUpdatedAt())
                .createdBy(sensor.getCreatedBy())
                .build();
    }

    /**
     * Map Sensor entity sang GeoJSON Feature
     */
    public SensorMapResponse.Feature toMapFeature(Sensor sensor) {
        return SensorMapResponse.Feature.builder()
                .type("Feature")
                .geometry(SensorMapResponse.Geometry.builder()
                        .type("Point")
                        .coordinates(Arrays.asList(sensor.getLon(), sensor.getLat()))
                        .build())
                .properties(SensorMapResponse.Properties.builder()
                        .sensorId(sensor.getSensorId())
                        .name(sensor.getName())
                        .status(sensor.getStatus())
                        .locationName(sensor.getLocationName())
                        .batteryLevel(sensor.getBatteryLevel())
                        .signalStrength(sensor.getSignalStrength())
                        .build())
                .build();
    }

    /**
     * Map danh sách Sensor sang GeoJSON FeatureCollection
     */
    public SensorMapResponse toMapResponse(List<Sensor> sensors) {
        List<SensorMapResponse.Feature> features = sensors.stream()
                .map(this::toMapFeature)
                .collect(Collectors.toList());

        return SensorMapResponse.builder()
                .type("FeatureCollection")
                .features(features)
                .build();
    }

    /**
     * Map SensorLog entity sang SensorLogResponse
     */
    public SensorLogResponse toLogResponse(SensorLog log) {
        return SensorLogResponse.builder()
                .id(log.getId())
                .sensorId(log.getSensorId())
                .action(log.getAction())
                .performedBy(log.getPerformedBy())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .comment(log.getComment())
                .createdAt(log.getCreatedAt())
                .build();
    }

    /**
     * Map danh sách SensorLog
     */
    public List<SensorLogResponse> toLogResponses(List<SensorLog> logs) {
        return logs.stream()
                .map(this::toLogResponse)
                .collect(Collectors.toList());
    }
}
