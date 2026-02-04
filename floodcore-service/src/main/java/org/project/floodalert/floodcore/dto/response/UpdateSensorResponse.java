package org.project.floodalert.floodcore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateSensorResponse {
    UUID id;
    String sensorId;
    String name;
    String locationName;
    BigDecimal lat;
    BigDecimal lon;
    String status;
    BigDecimal warningThreshold;
    BigDecimal dangerThreshold;
    String hardwareModel;
    String firmwareVersion;
    LocalDateTime updatedAt;

    // Thông tin về những gì đã thay đổi
    List<String> changedFields;
    String message;
}
