package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.model.Sensor;

import java.util.List;

public interface SensorSyncService {
    /**
     * Đồng bộ danh sách sensors lên Redis cache
     */
    void syncSensorsToCache(List<Sensor> sensors);
}
