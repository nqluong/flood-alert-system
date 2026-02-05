package org.project.floodalert.ingestion.service;

public interface SensorBlacklistService {
    boolean isSensorBlacklisted(String sensorId);
}
