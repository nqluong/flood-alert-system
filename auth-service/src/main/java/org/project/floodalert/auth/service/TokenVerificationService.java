package org.project.floodalert.auth.service;

import org.project.floodalert.auth.dto.response.VerifyTokenResponse;

public interface TokenVerificationService {
    VerifyTokenResponse verifyToken(String token);

    boolean isTokenValid(String token);
}
