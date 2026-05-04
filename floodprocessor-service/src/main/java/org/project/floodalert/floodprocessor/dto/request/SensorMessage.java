package org.project.floodalert.floodprocessor.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Message nhận từ Kafka topic
 * Chứa sensor data đã được parse và validate ở ingestion-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorMessage {
    
    @JsonProperty("sensor_id")
    String sensorId;
    
    @JsonProperty("sensor_data")
    SensorRaw sensorData;  // DTO đã validated (thay vì rawPayload String)
    
    @JsonProperty("topic")
    String topic;
    
    @JsonProperty("received_at")
    Instant receivedAt;
}
