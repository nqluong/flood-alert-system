package org.project.floodalert.floodcore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualSensorProvisionRequest {
    
    @NotNull(message = "target_count không được null")
    @Min(value = 1, message = "target_count phải >= 1")
    @Max(value = 1000, message = "target_count không được vượt quá 1000")
    private Integer targetCount;
}
