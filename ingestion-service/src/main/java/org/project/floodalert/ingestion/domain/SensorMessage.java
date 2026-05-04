package org.project.floodalert.ingestion.domain;

import lombok.Builder;
import lombok.Value;
import org.project.floodalert.ingestion.dto.SensorDataDTO;

import java.time.Instant;

/**
 * Message được publish lên Kafka
 * Chứa sensor data đã được parse và validate
 */
@Builder
@Value
public class SensorMessage {
    String sensorId;
    SensorDataDTO sensorData;  // DTO đã validated thay vì rawPayload
    String topic;
    Instant receivedAt;
}
