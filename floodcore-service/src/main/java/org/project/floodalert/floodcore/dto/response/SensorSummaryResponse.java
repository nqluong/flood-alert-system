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
public class SensorSummaryResponse {
    UUID id;
    String sensorId;
    String name;
    String locationName;
    BigDecimal lat;
    BigDecimal lon;
    String status;

    // Thông tin trạng thái
    Integer batteryLevel;
    Integer signalStrength;
    LocalDateTime lastHeartbeat;
    LocalDateTime lastReadingAt;

    // Thông tin metadata
    String hardwareModel;
    String firmwareVersion;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
