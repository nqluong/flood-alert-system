package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.dto.request.UpdateSensorRequest;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.service.SensorChangeTrackingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SensorChangeTrackingServiceImpl implements SensorChangeTrackingService {

    @Override
    public List<String> detectChangedFields(Sensor oldSensor, UpdateSensorRequest request) {
        List<String> changedFields = new ArrayList<>();

        // Kiểm tra từng field
        if (hasChanged(oldSensor.getName(), request.getName())) {
            changedFields.add("name");
        }

        if (hasChanged(oldSensor.getLocationName(), request.getLocationName())) {
            changedFields.add("locationName");
        }

        if (hasChanged(oldSensor.getLat(), request.getLat())) {
            changedFields.add("lat");
        }

        if (hasChanged(oldSensor.getLon(), request.getLon())) {
            changedFields.add("lon");
        }

        if (hasChanged(oldSensor.getWarningThreshold(), request.getWarningThreshold())) {
            changedFields.add("warningThreshold");
        }

        if (hasChanged(oldSensor.getDangerThreshold(), request.getDangerThreshold())) {
            changedFields.add("dangerThreshold");
        }

        if (hasChanged(oldSensor.getHardwareModel(), request.getHardwareModel())) {
            changedFields.add("hardwareModel");
        }

        if (hasChanged(oldSensor.getFirmwareVersion(), request.getFirmwareVersion())) {
            changedFields.add("firmwareVersion");
        }

        return changedFields;
    }

    @Override
    public Map<String, Object> createOldValueSnapshot(Sensor sensor, List<String> changedFields) {
        Map<String, Object> snapshot = new HashMap<>();

        for (String field : changedFields) {
            Object value = getFieldValue(sensor, field);
            if (value != null) {
                snapshot.put(field, value);
            }
        }

        return snapshot;
    }

    @Override
    public Map<String, Object> createNewValueSnapshot(Sensor sensor, List<String> changedFields) {
        Map<String, Object> snapshot = new HashMap<>();

        for (String field : changedFields) {
            Object value = getFieldValue(sensor, field);
            if (value != null) {
                snapshot.put(field, value);
            }
        }

        return snapshot;
    }

    @Override
    public boolean isLocationUpdate(List<String> changedFields) {
        return changedFields.contains("lat") || changedFields.contains("lon");
    }

    @Override
    public boolean isThresholdUpdate(List<String> changedFields) {
        return changedFields.contains("warningThreshold") ||
                changedFields.contains("dangerThreshold");
    }

    private boolean hasChanged(Object oldValue, Object newValue) {
        if (newValue == null) {
            return false;
        }

        if (oldValue == null) {
            return true;
        }

        return !oldValue.equals(newValue);
    }

    private Object getFieldValue(Sensor sensor, String fieldName) {
        return switch (fieldName) {
            case "name" -> sensor.getName();
            case "locationName" -> sensor.getLocationName();
            case "lat" -> sensor.getLat();
            case "lon" -> sensor.getLon();
            case "warningThreshold" -> sensor.getWarningThreshold();
            case "dangerThreshold" -> sensor.getDangerThreshold();
            case "hardwareModel" -> sensor.getHardwareModel();
            case "firmwareVersion" -> sensor.getFirmwareVersion();
            case "status" -> sensor.getStatus();
            default -> null;
        };
    }
}
