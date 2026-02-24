package org.project.floodalert.floodprocessor.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.time.Instant;

/**
 * DTO ánh xạ message nhận từ ingestion-service qua Kafka topic ingest.
 * rawPayload chứa JSON string của SensorRaw gốc từ thiết bị.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorMessage {
    String sensorId;
    String rawPayload;
    String topic;
    Instant receivedAt;
}
