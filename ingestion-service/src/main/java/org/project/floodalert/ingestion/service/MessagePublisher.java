package org.project.floodalert.ingestion.service;

import org.project.floodalert.ingestion.domain.SensorMessage;

public interface MessagePublisher {
    void publish(SensorMessage sensorMessage);
}
