package org.project.floodalert.notification.service;

import java.util.UUID;

public interface DeviceTokenService {
    void saveToken(UUID userId, String token);

    void removeToken(UUID userId);

    void handleFcmTokenEvent(UUID userId, String token, Boolean isActive);
}
