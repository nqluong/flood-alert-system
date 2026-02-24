package org.project.floodalert.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessedSensorData {

    String sensorId;
    Double waterLevel;
    Double lat;
    Double lon;
    Double battery;
    Long timestamp;
    Double warningThreshold;
    Double dangerThreshold;
    String locationName;
    String status;
    String previousStatus;
    String readingId;
    Integer signalStrength;
    Double temperature;
    Double humidity;

    boolean stateChanged;
}
