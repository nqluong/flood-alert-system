package org.project.floodalert.floodprocessor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemConfigService {

    @Value("${system.config.sensor-timeout-ms:900000}")
    private long sensorTimeoutMs;

    /**
     * Retrieves the sensor timeout value in milliseconds.
     *
     * @return the sensor timeout value
     */
    public long getSensorTimeoutMs() {
        return sensorTimeoutMs;
    }
}
