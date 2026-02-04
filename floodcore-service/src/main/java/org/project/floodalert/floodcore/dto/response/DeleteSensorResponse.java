package org.project.floodalert.floodcore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeleteSensorResponse {
    UUID id;
    String sensorId;
    String deleteType;
    String status;
    String message;
    LocalDateTime deletedAt;
    Boolean removedFromMap;
    Boolean removedFromRedis;
}
