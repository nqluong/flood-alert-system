package org.project.floodalert.floodprocessor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private String apiKey;

    private String baseUrl;

    private String model;

    private int timeoutSeconds = 60;

    private RetryConfig retry = new RetryConfig();

    @Data
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long initialDelayMs = 1000;
        private long maxDelayMs = 5000;
        private double multiplier = 2.0;
    }
}
