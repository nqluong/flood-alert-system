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
public class SensorDetailResponse {
    // Thông tin cơ bản
    UUID id;
    String sensorId;
    String name;
    String locationName;
    BigDecimal lat;
    BigDecimal lon;
    String status;

    // Ngưỡng cảnh báo
    BigDecimal warningThreshold;
    BigDecimal dangerThreshold;

    // Thông tin phần cứng
    String hardwareModel;
    String firmwareVersion;
    Integer batteryLevel;
    Integer signalStrength;

    // Thông tin thời gian
    LocalDateTime installedAt;
    LocalDateTime lastHeartbeat;
    LocalDateTime lastReadingAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // Thông tin người tạo
    UUID createdBy;

    // Lịch sử thay đổi (nếu được yêu cầu)
    List<SensorLogResponse> logs;
}
