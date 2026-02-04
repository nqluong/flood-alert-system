package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.model.SensorLog;
import org.project.floodalert.floodcore.repository.SensorLogRepository;
import org.project.floodalert.floodcore.service.SensorLogService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorLogServiceImpl implements SensorLogService {

    private final SensorLogRepository sensorLogRepository;

    @Override
    public void logSensorCreated(Sensor sensor, UUID performedBy) {
        log.info("Ghi log tạo sensor: {}", sensor.getSensorId());

        Map<String, Object> newValue = buildSensorValueMap(sensor);

        SensorLog sensorLog = SensorLog.builder()
                .sensorId(sensor.getId())
                .action("CREATED")
                .performedBy(performedBy)
                .oldValue(null)
                .newValue(newValue)
                .comment("Khởi tạo cảm biến mới vào hệ thống")
                .build();

        sensorLogRepository.save(sensorLog);
    }

    @Override
    public void logSensorUpdated(UUID sensorId, String action,
                                 Map<String, Object> oldValue,
                                 Map<String, Object> newValue,
                                 UUID performedBy) {
        log.info("Ghi log cập nhật sensor ID: {}, action: {}", sensorId, action);

        SensorLog sensorLog = SensorLog.builder()
                .sensorId(sensorId)
                .action(action)
                .performedBy(performedBy)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        sensorLogRepository.save(sensorLog);
    }

    @Override
    public void logWithComment(UUID sensorId, String action,
                               Map<String, Object> oldValue,
                               Map<String, Object> newValue,
                               String comment, UUID performedBy) {
        log.info("Ghi log với comment cho sensor ID: {}, action: {}", sensorId, action);

        SensorLog sensorLog = SensorLog.builder()
                .sensorId(sensorId)
                .action(action)
                .performedBy(performedBy)
                .oldValue(oldValue)
                .newValue(newValue)
                .comment(comment)
                .build();

        sensorLogRepository.save(sensorLog);
    }

    private Map<String, Object> buildSensorValueMap(Sensor sensor) {
        Map<String, Object> map = new HashMap<>();
        map.put("sensorId", sensor.getSensorId());
        map.put("name", sensor.getName());
        map.put("locationName", sensor.getLocationName());
        map.put("lat", sensor.getLat());
        map.put("lon", sensor.getLon());
        map.put("status", sensor.getStatus());
        map.put("warningThreshold", sensor.getWarningThreshold());
        map.put("dangerThreshold", sensor.getDangerThreshold());
        map.put("hardwareModel", sensor.getHardwareModel());
        map.put("firmwareVersion", sensor.getFirmwareVersion());
        return map;
    }
}
