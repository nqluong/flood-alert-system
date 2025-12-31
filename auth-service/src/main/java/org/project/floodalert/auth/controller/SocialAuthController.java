package org.project.floodalert.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.dto.request.LoginRequest;
import org.project.floodalert.auth.dto.request.SocialLoginRequest;
import org.project.floodalert.auth.dto.response.LoginResponse;
import org.project.floodalert.auth.service.SocialAuthService;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SocialAuthController {
    private final SocialAuthService socialAuthService;

    @PostMapping("/social/google")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithGoogle(
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletRequest httpRequest) {

        request.setProvider("GOOGLE");

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Google login attempt from IP: {}", ipAddress);

        LoginResponse response = socialAuthService.authenticateWithSocial(
                request, ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/social/facebook")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithFacebook(
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletRequest httpRequest) {

        request.setProvider("FACEBOOK");

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Facebook login attempt from IP: {}", ipAddress);

        LoginResponse response = socialAuthService.authenticateWithSocial(
                request, ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/social/login")
    public ResponseEntity<ApiResponse<LoginResponse>> socialLogin(
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Social login attempt: provider={}, IP={}", request.getProvider(), ipAddress);

        LoginResponse response = socialAuthService.authenticateWithSocial(
                request, ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
