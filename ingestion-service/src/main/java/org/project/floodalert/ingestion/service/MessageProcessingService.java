package org.project.floodalert.ingestion.service;

public interface MessageProcessingService {
    void process(String topic, String payload);
}
