package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.dto.request.UpdateSensorRequest;
import org.project.floodalert.floodcore.model.Sensor;

import java.util.List;
import java.util.Map;

public interface SensorChangeTrackingService {
    /**
     * Phát hiện các fields đã thay đổi
     * @param oldSensor Sensor trước khi update
     * @param request Update request
     * @return Danh sách tên các fields đã thay đổi
     */
    List<String> detectChangedFields(Sensor oldSensor, UpdateSensorRequest request);

    /**
     * Tạo snapshot của giá trị cũ
     * @param sensor Sensor entity
     * @param changedFields Các fields đã thay đổi
     * @return Map chứa old values
     */
    Map<String, Object> createOldValueSnapshot(Sensor sensor, List<String> changedFields);

    /**
     * Tạo snapshot của giá trị mới
     * @param sensor Sensor entity sau update
     * @param changedFields Các fields đã thay đổi
     * @return Map chứa new values
     */
    Map<String, Object> createNewValueSnapshot(Sensor sensor, List<String> changedFields);

    boolean isLocationUpdate(List<String> changedFields);

    boolean isThresholdUpdate(List<String> changedFields);
}
