package org.project.floodalert.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Telemetry {
    @NotNull(message = "water_level không được null")
    @JsonProperty("water_level")
    Double waterLevel;

    @JsonProperty("distance_raw")
    Double distanceRaw;

    @JsonProperty("velocity")
    Double velocity;

    @JsonProperty("lat")
    Double lat;

    @JsonProperty("lon")
    Double lon;
}
