package org.project.floodalert.floodcore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchCreateSensorResponse {
    
    int totalRequested;
    int successCount;
    int failureCount;
    
    List<CreateSensorResult> results;
    
    String message;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CreateSensorResult {
        boolean success;
        String sensorId;
        CreateSensorResponse data;
        String errorMessage;
    }
}
