package org.project.floodalert.floodcore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorLogResponse {
    UUID id;
    UUID sensorId;
    String action;
    UUID performedBy;
    Map<String, Object> oldValue;
    Map<String, Object> newValue;
    String comment;
    LocalDateTime createdAt;
}
