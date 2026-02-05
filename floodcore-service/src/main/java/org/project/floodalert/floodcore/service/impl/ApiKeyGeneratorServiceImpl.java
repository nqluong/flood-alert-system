package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.project.floodalert.floodcore.service.ApiKeyGeneratorService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyGeneratorServiceImpl implements ApiKeyGeneratorService {

    private static final int API_KEY_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SensorRepository sensorRepository;
    @Override
    public String generateApiKey() {
        byte[] randomBytes = new byte[API_KEY_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        String apiKey = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        return apiKey;
    }

    @Override
    public boolean isApiKeyExists(String apiKey) {
        return sensorRepository.existsByApiKey(apiKey);
    }

    @Override
    public String generateUniqueApiKey() {
        String apiKey;
        int attempts = 0;
        int maxAttempts = 5;

        do {
            apiKey = generateApiKey();
            attempts++;

            if (attempts >= maxAttempts) {
                log.error("Không thể sinh API Key unique sau {} lần thử", maxAttempts);
                throw new RuntimeException("Không thể sinh API Key unique");
            }
        } while (isApiKeyExists(apiKey));

        log.info("Đã sinh API Key unique sau {} lần thử", attempts);
        return apiKey;
    }
}
