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

    private int timeoutSeconds = 5;
}
