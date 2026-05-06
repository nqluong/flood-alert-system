package org.project.floodalert.floodprocessor.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorMessage {
    
    String sensorId;
    
    SensorRaw sensorData;
    
    String topic;
    
    Instant receivedAt;
}
