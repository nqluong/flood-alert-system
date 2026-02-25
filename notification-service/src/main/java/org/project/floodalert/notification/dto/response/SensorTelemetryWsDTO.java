package org.project.floodalert.notification.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorTelemetryWsDTO {
    String sensorId;
    Double waterLevel;
    Double lat;
    Double lon;
    Double battery;
    Long timestamp;
    Double warningThreshold;
    Double dangerThreshold;
    String locationName;
}
