package org.project.floodalert.floodcore.service;

public interface ApiKeyGeneratorService {
    /**
     * Tạo một API key mới cho sensor
     * @return API key mới
     */
    String generateApiKey();

    boolean isApiKeyExists(String apiKey);

    String generateUniqueApiKey();

}
