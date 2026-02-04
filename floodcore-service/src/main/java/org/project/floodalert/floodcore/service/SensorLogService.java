package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.model.Sensor;

import java.util.Map;
import java.util.UUID;

public interface SensorLogService {
    /**
     * Ghi log khi tạo sensor mới
     */
    void logSensorCreated(Sensor sensor, UUID performedBy);

    /**
     * Ghi log khi cập nhật sensor
     */
    void logSensorUpdated(UUID sensorId, String action,
                          Map<String, Object> oldValue,
                          Map<String, Object> newValue,
                          UUID performedBy);

    /**
     * Ghi log với comment
     */
    void logWithComment(UUID sensorId, String action,
                        Map<String, Object> oldValue,
                        Map<String, Object> newValue,
                        String comment,
                        UUID performedBy);
}
