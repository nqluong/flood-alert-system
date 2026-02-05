package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.model.Sensor;

import java.util.List;

public interface SensorMetadataWriter {
    /**
     * Ghi metadata của nhiều sensors vào Redis
     */
    void batchWriteSensorMetadata(List<Sensor> sensors);
}
