package org.project.floodalert.floodcore.mapper;

import org.project.floodalert.floodcore.dto.request.CreateSensorRequest;
import org.project.floodalert.floodcore.dto.response.CreateSensorResponse;
import org.project.floodalert.floodcore.model.Sensor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SensorMapper {

     /**
     * Chuyển từ CreateSensorRequest sang Sensor Entity
     */
    public Sensor toEntity(CreateSensorRequest request, String apiKey) {
        return Sensor.builder()
                .sensorId(request.getSensorId())
                .name(request.getName())
                .locationName(request.getLocationName())
                .lat(request.getLat())
                .lon(request.getLon())
                .status("ACTIVE")
                .apiKey(apiKey)
                .warningThreshold(request.getWarningThreshold())
                .dangerThreshold(request.getDangerThreshold())
                .hardwareModel(request.getHardwareModel())
                .firmwareVersion(request.getFirmwareVersion())
                .installedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Chuyển từ Sensor Entity sang CreateSensorResponse
     */
    public CreateSensorResponse toCreateResponse(Sensor sensor) {
        return CreateSensorResponse.builder()
                .id(sensor.getId())
                .sensorId(sensor.getSensorId())
                .name(sensor.getName())
                .locationName(sensor.getLocationName())
                .lat(sensor.getLat())
                .lon(sensor.getLon())
                .status(sensor.getStatus())
                .apiKey(sensor.getApiKey())
                .warningThreshold(sensor.getWarningThreshold())
                .dangerThreshold(sensor.getDangerThreshold())
                .hardwareModel(sensor.getHardwareModel())
                .firmwareVersion(sensor.getFirmwareVersion())
                .createdAt(sensor.getCreatedAt())
                .message("Cảm biến đã được tạo thành công. Vui lòng lưu API Key để cấu hình cho thiết bị.")
                .build();
    }
}
