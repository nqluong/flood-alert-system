package org.project.floodalert.floodcore.service.mapper;

import org.project.floodalert.floodcore.model.Sensor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SensorMetadataMapper {

    public Map<String, String> toMetadataMap(Sensor sensor) {
        Map<String, String> metadata = new HashMap<>();

        // Thông tin cơ bản
        metadata.put("status", getOrDefault(sensor.getSTATUS(), "UNKNOWN"));
        metadata.put("name", getOrDefault(sensor.getNAME(), ""));
        metadata.put("location_name", getOrDefault(sensor.getLOCATION_NAME(), ""));

        // Thông tin phần cứng
        metadata.put("hardware_model", getOrDefault(sensor.getHARDWARE_MODEL(), ""));
        metadata.put("firmware_version", getOrDefault(sensor.getFIRMWARE_VERSION(), ""));

        // Pin và tín hiệu
        metadata.put("battery_level", sensor.getBATTERY_LEVEL() != null
                ? sensor.getBATTERY_LEVEL().toString() : "0");
        metadata.put("signal_strength", sensor.getSIGNAL_STRENGTH() != null
                ? sensor.getSIGNAL_STRENGTH().toString() : "0");

        // Timestamp
        metadata.put("last_heartbeat", sensor.getLAST_HEARTBEAT() != null
                ? sensor.getLAST_HEARTBEAT().toString() : "");
        metadata.put("last_reading_at", sensor.getLAST_READING_AT() != null
                ? sensor.getLAST_READING_AT().toString() : "");

        // Tọa độ
        metadata.put("lat", sensor.getLAT() != null
                ? sensor.getLAT().toString() : "0");
        metadata.put("lon", sensor.getLON() != null
                ? sensor.getLON().toString() : "0");

        return metadata;
    }

    private String getOrDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
