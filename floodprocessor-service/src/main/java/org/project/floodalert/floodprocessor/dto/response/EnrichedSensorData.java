package org.project.floodalert.floodprocessor.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnrichedSensorData {
    String sensorId;
    String model;
    String firmwareVer;

    Double waterLevel;
    Double lat;
    Double lon;
    Long timestamp;

    Double warningThreshold;
    Double dangerThreshold;
    String locationName;

    Double battery;
    String deviceStatus;
    Double temperature;
    Integer signalStrength;

}
