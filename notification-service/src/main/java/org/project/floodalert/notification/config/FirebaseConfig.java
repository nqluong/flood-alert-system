package org.project.floodalert.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.service-account-path:firebase-service-account.json}")
    private String serviceAccountPath;

    @Value("${app.firebase.enabled:true}")
    private boolean firebaseEnabled;

    @Autowired
    private ResourceLoader resourceLoader;

    @PostConstruct
    public void initialize() {
        if (!firebaseEnabled) {
            log.warn("Firebase is DISABLED in configuration. Push notifications will not work.");
            return;
        }

        try {
            // Check if FirebaseApp is already initialized
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase already initialized");
                return;
            }

            // Load service account credentials
            InputStream serviceAccount = loadServiceAccountFile();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);

            log.info("Firebase Admin SDK initialized successfully");
            log.info("FCM Push Notification service is ready");

        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
            log.error("Make sure '{}' exists in classpath or is configured correctly", serviceAccountPath);
            log.error("Push notifications will NOT work!");
        }
    }

    private InputStream loadServiceAccountFile() throws IOException {
        try {
            Resource resource = resourceLoader.getResource(serviceAccountPath);
            if (resource.exists()) {
                log.info("Loading Firebase credentials from classpath: {}", serviceAccountPath);
                return resource.getInputStream();
            }
        } catch (Exception e) {
            log.debug("Service account not found in classpath, trying file system", e);
        }

        try {
            log.info("Loading Firebase credentials from file system: {}", serviceAccountPath);
            return new FileInputStream(serviceAccountPath);
        } catch (IOException e) {
            log.error("Cannot find Firebase service account file: {}", serviceAccountPath);
            throw e;
        }
    }
}
