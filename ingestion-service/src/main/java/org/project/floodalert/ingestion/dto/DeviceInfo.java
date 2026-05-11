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
public class DeviceInfo {
    @NotNull(message = "sensor_id không được null")
    @JsonProperty("sensor_id")
    String sensorId;

    @JsonProperty("model")
    String model;

    @JsonProperty("firmware_ver")
    String firmwareVer;

    @JsonProperty("message_id")
    String messageId;

    @JsonProperty("api_key")
    String apiKey;
}
