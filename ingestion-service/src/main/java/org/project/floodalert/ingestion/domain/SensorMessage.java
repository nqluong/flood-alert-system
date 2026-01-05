package org.project.floodalert.ingestion.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Builder
@Value
public class SensorMessage {
    String sensorId;
    String rawPayload;
    String topic;
    Instant receivedAt;
}
