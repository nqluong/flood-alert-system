package org.project.floodalert.ingestion.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.ingestion.service.SensorIdExtractor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TopicBasedSensorIdExtractor implements SensorIdExtractor {

    private static final String EXPECTED_PREFIX = "floodguard/sensors/";
    private static final String EXPECTED_SUFFIX = "/telemetry";

    @Override
    public String extract(String topic) {
        if(topic == null || topic.isEmpty()) {
            log.warn("Topic is null or empty");
            return "";
        }

        String[] parts = topic.split("/");
        if(parts.length != 4){
            log.warn("Invalid topic format: expected 4 parts, got {}", parts.length);
            return null;
        }

        if(!"floodguard".equals(parts[0]) || !"sensors".equals(parts[1]) || !"telemetry".equals(parts[3])){
            log.warn("Invalid topic structure: {}", topic);
            return null;
        }

        String sensorId = parts[2];
        if(sensorId.isEmpty()){
            log.warn("Sensor ID is empty in topic: {}", topic);
            return null;
        }

        return sensorId;
    }
}
