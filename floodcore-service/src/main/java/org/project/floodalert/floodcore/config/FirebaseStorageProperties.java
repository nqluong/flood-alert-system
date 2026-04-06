package org.project.floodalert.floodcore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "firebase.storage")
public class FirebaseStorageProperties {
    private String bucketName;
    private String serviceAccountPath;
    private int signedUrlDurationMinutes = 15;
}
