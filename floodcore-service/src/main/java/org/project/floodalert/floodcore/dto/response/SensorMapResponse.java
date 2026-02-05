package org.project.floodalert.floodcore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorMapResponse {
    String type = "FeatureCollection";
    List<Feature> features;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Feature {
        String type = "Feature";
        Geometry geometry;
        Properties properties;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Geometry {
        String type = "Point";
        List<BigDecimal> coordinates; // [lon, lat]
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Properties {
        String sensorId;
        String name;
        String status;
        String locationName;
        Integer batteryLevel;
        Integer signalStrength;
    }
}
