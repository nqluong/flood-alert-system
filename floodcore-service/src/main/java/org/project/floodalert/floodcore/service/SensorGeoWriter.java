package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.model.Sensor;

import java.util.List;

public interface SensorGeoWriter {
    /**
     * Ghi tọa độ địa lý của nhiều sensors vào Redis
     */
    void batchWriteSensorGeoLocation(List<Sensor> sensors);
}
