package org.project.floodalert.auth.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.config.JwtProperties;
import org.project.floodalert.auth.constants.RoleConstants;
import org.project.floodalert.auth.dto.request.LoginRequest;
import org.project.floodalert.auth.dto.request.RefreshTokenRequest;
import org.project.floodalert.auth.dto.request.RegisterRequest;
import org.project.floodalert.auth.dto.response.LoginResponse;
import org.project.floodalert.auth.dto.response.UserResponse;
import org.project.floodalert.auth.enums.AuthProvider;
import org.project.floodalert.auth.enums.TokenType;
import org.project.floodalert.auth.enums.UserStatus;
import org.project.floodalert.auth.exception.AuthErrorCode;
import org.project.floodalert.auth.model.InvalidatedToken;
import org.project.floodalert.auth.model.User;
import org.project.floodalert.auth.repository.InvalidatedTokenRepository;
import org.project.floodalert.auth.repository.UserRepository;
import org.project.floodalert.auth.repository.UserRoleRepository;
import org.project.floodalert.auth.security.JwtTokenGenerator;
import org.project.floodalert.auth.security.JwtTokenValidator;
import org.project.floodalert.auth.service.AuthService;
import org.project.floodalert.auth.service.RoleService;
import org.project.floodalert.auth.utils.AuditLogger;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {

    UserRepository userRepository;
    UserRoleRepository userRoleRepository;
    PasswordEncoder passwordEncoder;
    JwtTokenGenerator jwtTokenGenerator;
    JwtTokenValidator jwtTokenValidator;
    JwtProperties jwtProperties;
    AuditLogger auditLogger;
    RoleService roleService;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @Override
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = findUserByEmail(request.getEmail());

        validateUserStatus(user);
        validateAuthProvider(user);
        validatePassword(request.getPassword(), user.getPasswordHash());

        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());

        String accessToken = jwtTokenGenerator.generateAccessToken(user, roles);
        String refreshToken = jwtTokenGenerator.generateRefreshToken(user.getId());

        updateLastLogin(user);

//        auditLogger.logLogin(user.getId(), ipAddress, userAgent);

        log.info("User logged in successfully: {}", user.getEmail());

        return buildLoginResponse(accessToken, refreshToken, user, roles);
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(AuthErrorCode.EMAIL_ALREADY_EXISTS, "Email đã được sử dụng");
        }

        //  Kiểm tra số điện thoại (nếu có)
        if (request.getPhoneNumber() != null &&
                userRepository.existsByPhone(request.getPhoneNumber())) {
            throw new AppException(AuthErrorCode.PHONE_ALREADY_EXISTS, "Số điện thoại đã được sử dụng");
        }

        User newUser = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhoneNumber())
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(newUser);

        assignDefaultRole(savedUser.getId());

        auditLogger.logUserRegistration(
                savedUser.getId(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        log.info("New user registered successfully: {}", savedUser.getEmail());

        List<String> roles = List.of("USER");
        return buildUserResponse(savedUser, roles);
    }

    @Override
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {

        String refreshTokenStr = request.getRefreshToken();

        // Validate refresh token
        if (!jwtTokenValidator.validateToken(refreshTokenStr)) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Refresh token không hợp lệ");
        }
        String tokenType = jwtTokenValidator.getTokenType(refreshTokenStr);
        if (!"refresh_token".equals(tokenType)) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Token type không đúng");
        }

        UUID userId = jwtTokenValidator.getUserIdFromToken(refreshTokenStr);
        User user = findUserById(userId);
        validateUserStatus(user);

        invalidateToken(refreshTokenStr, userId, "TOKEN_REFRESH", null, null);


        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        String newAccessToken = jwtTokenGenerator.generateAccessToken(user, roles);
        String newRefreshToken = jwtTokenGenerator.generateRefreshToken(user.getId());

        return buildLoginResponse(newAccessToken, newRefreshToken, user, roles);
    }

    @Override
    public void logout(String refreshTokenStr, UUID userId, String ipAddress, String userAgent) {

        invalidateToken(refreshTokenStr, userId, "USER_LOGOUT", ipAddress, userAgent);

        auditLogger.logLogout(userId, ipAddress, userAgent);

        log.info("User logged out successfully: {}", userId);
    }

    @Override
    public UserResponse getCurrentUser(UUID userId) {
        User user = findUserById(userId);
        List<String> roles = userRoleRepository.findRoleNamesByUserId(userId);
        return buildUserResponse(user, roles);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }
    }

    private void validateAuthProvider(User user) {
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new AppException(
                    ErrorCode.WRONG_AUTH_PROVIDER,
                    "Tài khoản này đăng nhập bằng " + user.getAuthProvider().name()
            );
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    private void updateLastLogin(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private LoginResponse buildLoginResponse(String accessToken, String refreshToken,
                                             User user, List<String> roles) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration())
                .userResponse(buildUserResponse(user, roles))
                .build();
    }

    private void assignDefaultRole(UUID userId) {
        try {
            roleService.assignRoleToUser(userId, RoleConstants.USER);
        } catch (Exception e) {
            log.error("Failed to assign default role to user: {}", userId, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Không thể gán quyền cho người dùng");
        }
    }

    private UserResponse buildUserResponse(User user, List<String> roles) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().name())
                .roles(roles)
                .build();
    }

    private void invalidateToken(String token, UUID userId, String reason,
                                 String ipAddress, String userAgent) {
        try {
            String jti = jwtTokenValidator.getJtiFromToken(token);
            LocalDateTime expiresAt = jwtTokenValidator.getExpirationFromToken(token);
            String tokenType = jwtTokenValidator.getTokenType(token);

            TokenType type = "access_token".equals(tokenType)
                    ? TokenType.ACCESS
                    : TokenType.REFRESH;

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .userId(userId)
                    .tokenJti(jti)
                    .tokenType(type)
                    .expiresAt(expiresAt)
                    .invalidatedReason(reason)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);

            log.debug("Token invalidated - JTI: {}, Reason: {}", jti, reason);
        } catch (Exception e) {
            log.error("Error invalidating token", e);
            // Don't throw exception, just log
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For can contain multiple IPs, get the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
