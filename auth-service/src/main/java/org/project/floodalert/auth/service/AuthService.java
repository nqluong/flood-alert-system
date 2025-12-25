package org.project.floodalert.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.project.floodalert.auth.dto.request.LoginRequest;
import org.project.floodalert.auth.dto.request.RefreshTokenRequest;
import org.project.floodalert.auth.dto.request.RegisterRequest;
import org.project.floodalert.auth.dto.response.LoginResponse;
import org.project.floodalert.auth.dto.response.UserResponse;

import java.util.UUID;

public interface AuthService {
    LoginResponse login(LoginRequest request, String ipAddress, String userAgent);

    UserResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    LoginResponse refreshAccessToken(RefreshTokenRequest request);

    void logout(String refreshToken, UUID userId, String ipAddress, String userAgent);

    UserResponse getCurrentUser(UUID userId);
}
