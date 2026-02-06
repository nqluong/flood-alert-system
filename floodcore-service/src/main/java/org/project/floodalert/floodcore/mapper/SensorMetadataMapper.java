package org.project.floodalert.floodcore.mapper;

import org.project.floodalert.floodcore.model.Sensor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class SensorMetadataMapper {

    public Map<String, String> toMetadataMap(Sensor sensor) {
        Map<String, String> metadata = new HashMap<>();

        // Thông tin cơ bản
        metadata.put("status", getOrDefault(sensor.getStatus(), "UNKNOWN"));
        metadata.put("name", getOrDefault(sensor.getName(), ""));
        metadata.put("location_name", getOrDefault(sensor.getLocationName(), ""));

        // Thông tin phần cứng
        metadata.put("hardware_model", getOrDefault(sensor.getHardwareModel(), ""));
        metadata.put("firmware_version", getOrDefault(sensor.getFirmwareVersion(), ""));

        // Pin và tín hiệu
        metadata.put("battery_level", sensor.getBatteryLevel() != null
                ? sensor.getBatteryLevel().toString() : "0");
        metadata.put("signal_strength", sensor.getSignalStrength() != null
                ? sensor.getSignalStrength().toString() : "0");

        // Timestamp
        metadata.put("last_heartbeat", sensor.getLastHeartbeat() != null
                ? sensor.getLastHeartbeat().toString() : "");
        metadata.put("last_reading_at", sensor.getLastReadingAt() != null
                ? sensor.getLastReadingAt().toString() : "");

        // Tọa độ
        metadata.put("lat", sensor.getLat() != null
                ? sensor.getLat().toString() : "0");
        metadata.put("lon", sensor.getLon() != null
                ? sensor.getLon().toString() : "0");
        metadata.put("warning_threshold", sensor.getWarningThreshold() != null
                ? Objects.requireNonNull(sensor.getWarningThreshold()).toString() : "0");
        metadata.put("danger_threshold", sensor.getDangerThreshold() != null
                ? Objects.requireNonNull(sensor.getDangerThreshold()).toString() : "0");
        return metadata;
    }

    private String getOrDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
