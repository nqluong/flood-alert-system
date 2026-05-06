package org.project.floodalert.floodprocessor.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorRaw {
    @JsonProperty("device_info")
    DeviceInfo deviceInfo;

    @JsonProperty("telemetry")
    Telemetry telemetry;

    @JsonProperty("health")
    Health health;

    @JsonProperty("timestamp")
    Long timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DeviceInfo {
        @JsonProperty("sensor_id")
        String sensorId;

        @JsonProperty("model")
        String model;

        @JsonProperty("firmware_ver")
        String firmwareVer;

        @JsonProperty("message_id")
        String messageId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Telemetry {
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Health {
        @JsonProperty("battery_level")
        Double batteryLevel;

        @JsonProperty("temperature")
        Double temperature;

        @JsonProperty("signal_strength")
        Integer signalStrength;

        @JsonProperty("status")
        String status;
    }
}
