package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.dto.request.CreateSensorRequest;

import java.math.BigDecimal;

public interface SensorValidationService {
    /**
     * Validate sensor ID đã tồn tại chưa
     */
    void validateSensorIdNotExists(String sensorId);

    /**
     * Validate ngưỡng cảnh báo và ngưỡng nguy hiểm
     */
    void validateThresholds(BigDecimal warningThreshold, BigDecimal dangerThreshold);

    /**
     * Validate tọa độ
     */
    void validateCoordinates(BigDecimal lat, BigDecimal lon);

    /**
     * Validate toàn bộ request
     */
    void validateCreateRequest(CreateSensorRequest request);
}
