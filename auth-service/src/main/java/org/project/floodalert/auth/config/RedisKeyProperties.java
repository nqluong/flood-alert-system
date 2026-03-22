package org.project.floodalert.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "redis")
public class RedisKeyProperties {

    private Keys keys = new Keys();
    private Ttl ttl = new Ttl();

    @Data
    public static class Keys {
        private UserKeys user = new UserKeys();

        @Data
        public static class UserKeys {
            private String reputation;
        }
    }

    @Data
    public static class Ttl {
        private UserTtl user = new UserTtl();

        @Data
        public static class UserTtl {
            private long reputation;
        }
    }
}
