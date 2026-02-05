package org.project.floodalert.floodcore.enums;

import java.util.List;

public enum SensorUpdateAction {

    THRESHOLD_UPDATED("Cập nhật ngưỡng cảnh báo"),
    LOCATION_MOVED("Di chuyển vị trí lắp đặt"),
    HARDWARE_UPDATED("Cập nhật thông tin phần cứng"),
    INFO_UPDATED("Cập nhật thông tin chung"),
    CONFIG_UPDATED("Cập nhật cấu hình"),
    STATUS_CHANGED("Thay đổi trạng thái"),
    MULTIPLE_FIELDS_UPDATED("Cập nhật nhiều trường");

    private final String description;

    SensorUpdateAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static SensorUpdateAction determineAction(List<String> changedFields) {
        if (changedFields.isEmpty()) {
            return INFO_UPDATED;
        }

        if (changedFields.size() > 3) {
            return MULTIPLE_FIELDS_UPDATED;
        }

        // Kiểm tra thay đổi vị trí
        if (changedFields.contains("lat") || changedFields.contains("lon")) {
            return LOCATION_MOVED;
        }

        // Kiểm tra thay đổi ngưỡng
        if (changedFields.contains("warningThreshold") || changedFields.contains("dangerThreshold")) {
            return THRESHOLD_UPDATED;
        }

        // Kiểm tra thay đổi phần cứng
        if (changedFields.contains("hardwareModel") || changedFields.contains("firmwareVersion")) {
            return HARDWARE_UPDATED;
        }

        // Kiểm tra thay đổi status
        if (changedFields.contains("status")) {
            return STATUS_CHANGED;
        }

        return CONFIG_UPDATED;
    }
}
