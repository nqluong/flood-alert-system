package org.project.floodalert.floodcore.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualSensorResponse {
    
    @JsonProperty("sensor_id")
    private String sensorId;
    
    @JsonProperty("api_key")
    private String apiKey;
    
    @JsonProperty("lat")
    private BigDecimal lat;
    
    @JsonProperty("lon")
    private BigDecimal lon;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("status")
    private String status;

    @JsonProperty("warning_threshold")
    private BigDecimal warningThreshold;

    @JsonProperty("danger_threshold")
    private BigDecimal dangerThreshold;
}
