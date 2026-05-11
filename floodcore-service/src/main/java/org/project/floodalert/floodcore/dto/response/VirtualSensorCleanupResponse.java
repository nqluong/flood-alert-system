package org.project.floodalert.floodcore.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualSensorCleanupResponse {
    
    @JsonProperty("deleted_count")
    private Integer deletedCount;
    
    @JsonProperty("message")
    private String message;
}
