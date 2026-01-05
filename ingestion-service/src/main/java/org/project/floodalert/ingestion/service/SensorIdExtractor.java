package org.project.floodalert.ingestion.service;

public interface SensorIdExtractor {
    String extract(String topic);
}
