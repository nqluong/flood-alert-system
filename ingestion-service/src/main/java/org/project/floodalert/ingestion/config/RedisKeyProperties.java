package org.project.floodalert.ingestion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "redis")
public class RedisKeyProperties {
    private Keys keys;
    private Ttl ttl;

    @Data
    public static class Keys{
        private SensorKeys sensor;
        private MapsKeys maps;
    }

    @Data
    public static class SensorKeys {
        private String info;
        private String status;
        private String metadata;
        private String blacklist;

        public String getInfoKey(String sensorId) {
            return info.replace("{sensorId}", sensorId);
        }

        public String getStatusKey(String sensorId) {
            return status.replace("{sensorId}", sensorId);
        }

        public String getMetadataKey(String sensorId) {
            return metadata.replace("{sensorId}", sensorId);
        }
    }

    @Data
    public static class MapsKeys {
        private String allSensors;
        private String activeSensors;
    }

    @Data
    public static class Ttl {
        private long cacheDefault;
        private long sensorInfo;
        private long mapCache;
        private long sensorList;
        private long sensorDetail;
    }
}
