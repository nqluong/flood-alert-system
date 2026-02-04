package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.model.Sensor;

import java.util.List;

public interface SensorBlacklistWriter {
    /**
     * Ghi danh sách sensor bị vô hiệu hóa (blacklist) vào Redis
     */
    void batchWriteSensorBlacklist(List<Sensor> sensors);
}
