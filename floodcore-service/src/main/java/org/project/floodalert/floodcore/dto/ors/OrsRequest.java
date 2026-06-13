package org.project.floodalert.floodcore.dto.ors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrsRequest {

    @JsonProperty("coordinates")
    List<List<Double>> coordinates;

    @JsonProperty("options")
    OrsOptions options;

    @JsonProperty("language")
    @Builder.Default
    String language = "vi";

    @JsonProperty("units")
    @Builder.Default
    String units = "m";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrsOptions {

        @JsonProperty("avoid_polygons")
        AvoidPolygons avoidPolygons;

        @JsonProperty("avoid_features")
        List<String> avoidFeatures;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AvoidPolygons {

        @JsonProperty("type")
        @Builder.Default
        String type = "MultiPolygon";

        // MultiPolygon coordinates: array of polygons; each polygon is an array of rings;
        // each ring is an array of [lon, lat] positions.
        @JsonProperty("coordinates")
        List<List<List<List<Double>>>> coordinates;
    }
}
