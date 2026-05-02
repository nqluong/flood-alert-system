package org.project.floodalert.floodcore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "integration.ors")
public class OrsProperties {

    private String apiUrl;
    private String apiKey;
}
