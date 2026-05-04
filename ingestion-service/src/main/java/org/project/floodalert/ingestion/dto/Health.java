package org.project.floodalert.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Health {
    @JsonProperty("battery_level")
    Double batteryLevel;

    @JsonProperty("temperature")
    Double temperature;

    @JsonProperty("signal_strength")
    Integer signalStrength;

    @JsonProperty("status")
    String status;
}
