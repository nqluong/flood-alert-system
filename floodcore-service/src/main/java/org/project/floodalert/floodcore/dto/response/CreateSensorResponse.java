package org.project.floodalert.floodcore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateSensorResponse {
    UUID id;
    String sensorId;
    String name;
    String locationName;
    BigDecimal lat;
    BigDecimal lon;
    String status;
    String apiKey;
    BigDecimal warningThreshold;
    BigDecimal dangerThreshold;
    String hardwareModel;
    String firmwareVersion;
    LocalDateTime createdAt;

    String message;
}
