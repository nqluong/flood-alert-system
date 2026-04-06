package org.project.floodalert.floodcore.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FirebaseStorageConfig {

    private final FirebaseStorageProperties properties;
    private final ResourceLoader resourceLoader;

    @Bean
    public Storage firebaseStorage() throws IOException {
        log.info("Initializing Firebase Storage with bucket: {}", properties.getBucketName());
        
        Resource resource = resourceLoader.getResource(properties.getServiceAccountPath());
        
        if (!resource.exists()) {
            throw new IllegalStateException(
                "Firebase service account file not found at: " + properties.getServiceAccountPath()
            );
        }

        try (InputStream serviceAccount = resource.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            
            return StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Storage", e);
            throw new IOException("Could not initialize Firebase Storage: " + e.getMessage(), e);
        }
    }
}
