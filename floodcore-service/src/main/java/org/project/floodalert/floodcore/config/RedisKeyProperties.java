package org.project.floodalert.floodcore.config;

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
        private CacheKeys cache;
        private FloodKeys flood;
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
    public static class CacheKeys {
        private String sensorList;
        private String sensorDetail;
        private String mapAll;
        private String mapActive;

        public String getSensorDetailKey(String sensorId) {
            return sensorDetail.replace("{sensorId}", sensorId);
        }
    }

    @Data
    public static class FloodKeys {
        /** Redis GEO key lưu spatial index – không có placeholder */
        private String geoIndex;
        /** Redis Hash key prefix cho flood detail – có {eventId} */
        private String detail;

        public String getDetailKey(String eventId) {
            return detail.replace("{eventId}", eventId);
        }
    }

    @Data
    public static class Ttl {
        private long cacheDefault;
        private long sensorInfo;
        private long mapCache;
        private long sensorList;
        private long sensorDetail;
        private long floodDetail;
    }
}
