package org.project.floodalert.floodprocessor.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorHealthSyncEvent {

    String sensorId;

    Double battery;

    Integer signalStrength;

    String status;

    Long timestamp;
}
