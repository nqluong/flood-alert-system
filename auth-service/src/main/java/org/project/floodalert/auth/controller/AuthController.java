package org.project.floodalert.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.auth.dto.request.LoginRequest;
import org.project.floodalert.auth.dto.request.RefreshTokenRequest;
import org.project.floodalert.auth.dto.request.RegisterRequest;
import org.project.floodalert.auth.dto.request.VerifyTokenRequest;
import org.project.floodalert.auth.dto.response.LoginResponse;
import org.project.floodalert.auth.dto.response.UserResponse;
import org.project.floodalert.auth.dto.response.VerifyTokenResponse;
import org.project.floodalert.auth.security.UserPrincipal;
import org.project.floodalert.auth.service.AuthService;
import org.project.floodalert.auth.service.TokenVerificationService;
import org.project.floodalert.auth.utils.HttpRequestUtils;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final TokenVerificationService tokenVerificationService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = HttpRequestUtils.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = authService.login(request, ipAddress, userAgent);

        return ResponseEntity.ok(
                ApiResponse.success("Đăng nhập thành công", response)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {


        UserResponse response = authService.register(request, httpRequest);

        return ResponseEntity.ok(
                ApiResponse.success("Đăng ký thành công", response)
        );
    }


    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        LoginResponse response = authService.refreshAccessToken(request);

        return ResponseEntity.ok(
                ApiResponse.success("Làm mới token thành công", response)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = UUID.fromString(principal.getUserId());

        String ipAddress = HttpRequestUtils.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        authService.logout(request.getRefreshToken(), userId, ipAddress, userAgent);

        return ResponseEntity.ok(
                ApiResponse.success("Đăng xuất thành công", null)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        UUID userId = UUID.fromString(principal.getUserId());
        UserResponse user = authService.getCurrentUser(userId);

        return ResponseEntity.ok(
                ApiResponse.success(user)
        );
    }

    @PostMapping("/verify-token")
    public ResponseEntity<ApiResponse<VerifyTokenResponse>> verifyToken(
            @Valid @RequestBody VerifyTokenRequest request) {

        VerifyTokenResponse response = tokenVerificationService.verifyToken(request.getToken());

        if (response.isValid()) {
            return ResponseEntity.ok(
                    ApiResponse.success("Token hợp lệ", response)
            );
        } else {
            return ResponseEntity.ok(
                    ApiResponse.<VerifyTokenResponse>builder()
                            .code(401)
                            .message(response.getInvalidReason())
                            .data(response)
                            .build()
            );
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyTokenResponse>> verifyTokenHeader(
            @RequestHeader("Authorization") String authHeader) {

        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token == null) {
            return ResponseEntity.ok(
                    ApiResponse.<VerifyTokenResponse>builder()
                            .code(401)
                            .message("Token không hợp lệ")
                            .data(VerifyTokenResponse.invalid("Missing token"))
                            .build()
            );
        }

        VerifyTokenResponse response = tokenVerificationService.verifyToken(token);

        if (response.isValid()) {
            return ResponseEntity.ok(
                    ApiResponse.success("Token hợp lệ", response)
            );
        } else {
            return ResponseEntity.ok(
                    ApiResponse.<VerifyTokenResponse>builder()
                            .code(401)
                            .message(response.getInvalidReason())
                            .data(response)
                            .build()
            );
        }
    }
}
