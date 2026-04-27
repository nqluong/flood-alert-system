package org.project.floodalert.auth.service;

import org.project.floodalert.auth.dto.request.FcmTokenRequest;

import java.util.UUID;

public interface FcmTokenService {

    void upsertToken(UUID userId, FcmTokenRequest request);

    void deactivateToken(UUID userId, String deviceId);

    void deactivateAllTokens(UUID userId);

    void cleanUpStaleTokens();
}
