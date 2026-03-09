package org.project.floodalert.floodprocessor.tracker;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SensorTelemetryTracker {

    private final ConcurrentHashMap<String, Long> sensorLastSeenMap = new ConcurrentHashMap<>();

    /**
     * Updates the last seen timestamp for a given sensor ID.
     *
     * @param sensorId the ID of the sensor
     */
    public void updateLastSeen(String sensorId) {
        sensorLastSeenMap.put(sensorId, System.currentTimeMillis());
    }

    /**
     * Retrieves the last seen timestamp for a given sensor ID.
     *
     * @param sensorId the ID of the sensor
     * @return the last seen timestamp, or null if the sensor ID is not tracked
     */
    public Long getLastSeen(String sensorId) {
        return sensorLastSeenMap.get(sensorId);
    }
}
