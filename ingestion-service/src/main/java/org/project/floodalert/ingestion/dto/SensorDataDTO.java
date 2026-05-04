package org.project.floodalert.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorDataDTO {
    
    @NotNull(message = "device_info không được null")
    @Valid
    @JsonProperty("device_info")
    DeviceInfo deviceInfo;

    @NotNull(message = "telemetry không được null")
    @Valid
    @JsonProperty("telemetry")
    Telemetry telemetry;

    @Valid
    @JsonProperty("health")
    Health health;

    @JsonProperty("timestamp")
    Long timestamp;
}
