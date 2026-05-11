package org.project.floodalert.ingestion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties cho validation rules
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.validation")
public class ValidationProperties {
    
    /**
     * Thời gian tối đa cho phép chênh lệch giữa timestamp và hiện tại (giây)
     * Default: 300 giây (5 phút)
     */
    private long maxTimestampDriftSeconds = 300;
    
    /**
     * Mức nước tối đa cho phép (cm)
     * Default: 1000 cm (10 mét)
     */
    private double maxWaterLevel = 1000.0;
    
    /**
     * Mức nước tối thiểu cho phép (cm)
     * Default: 0 cm
     */
    private double minWaterLevel = 0.0;
    
    /**
     * Redis key prefix cho sensor info
     */
    private String sensorInfoKeyPrefix = "sensor:info:";
    
    /**
     * Redis key prefix cho blacklist
     */
    private String blacklistKeyPrefix = "sensor:blacklist:";
}
