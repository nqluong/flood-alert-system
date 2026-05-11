package org.project.floodalert.ingestion.validation;

import lombok.Builder;
import lombok.Data;
import org.project.floodalert.ingestion.dto.SensorDataDTO;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ValidationContext {
    private String sensorId;
    private SensorDataDTO sensorData;
    private String topic;
    private Instant receivedAt;
    
    private Map<String, String> sensorInfo;
    
    private boolean valid;
    private String failureReason;
    private String failureStep;
}
