package org.project.floodalert.floodcore.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualSensorProvisionResponse {
    
    @JsonProperty("total_count")
    private Integer totalCount;
    
    @JsonProperty("created_count")
    private Integer createdCount;
    
    @JsonProperty("existing_count")
    private Integer existingCount;
    
    @JsonProperty("sensors")
    private List<VirtualSensorResponse> sensors;
    
    @JsonProperty("message")
    private String message;
}
