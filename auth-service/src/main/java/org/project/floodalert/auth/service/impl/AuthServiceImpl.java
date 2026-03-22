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
import org.project.floodalert.auth.model.UserProfile;
import org.project.floodalert.auth.repository.InvalidatedTokenRepository;
import org.project.floodalert.auth.repository.UserProfileRepository;
import org.project.floodalert.auth.repository.UserRepository;
import org.project.floodalert.auth.repository.UserRoleRepository;
import org.project.floodalert.auth.security.JwtTokenGenerator;
import org.project.floodalert.auth.security.JwtTokenValidator;
import org.project.floodalert.auth.service.AuthService;
import org.project.floodalert.auth.service.ReputationCacheService;
import org.project.floodalert.auth.service.RoleService;
import org.project.floodalert.auth.utils.AuditLogger;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.common.exception.BaseErrorCode;
import org.project.floodalert.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.project.floodalert.common.exception.ErrorCode.*;

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
    UserProfileRepository userProfileRepository;
    ReputationCacheService reputationCacheService;

    @Override
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = null;
        String loginMethod = null;

        try {
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                loginMethod = "EMAIL";
                user = findUserByEmail(request.getEmail());
            } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
                loginMethod = "PHONE";
                user = userRepository.findByPhone(request.getPhoneNumber())
                        .orElseThrow(() -> new AppException(INVALID_CREDENTIALS));
            }

            if (user == null) {
                throw new AppException(INVALID_CREDENTIALS);
            }

            // Validate user
            validateUserStatus(user);
            validateAuthProvider(user);
            validatePassword(request.getPassword(), user.getPasswordHash());

            // Generate tokens
            List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
            UserProfile profile = getUserProfile(user.getId()).orElse(null);
            String accessToken = jwtTokenGenerator.generateAccessToken(user, roles, getFullName(profile));
            String refreshToken = jwtTokenGenerator.generateRefreshToken(user.getId());

            // Update last login
            updateLastLogin(user);

            // Cache reputation score bất đồng bộ - không chặn luồng login
            if (profile != null) {
                reputationCacheService.cacheAsync(user.getId(), profile.getReputationScore());
            }

            // Log successful login
            auditLogger.logLogin(user.getId(), user.getEmail(), ipAddress, userAgent, loginMethod);

            log.info("User logged in successfully: email={}, method={}, ip={}",
                    user.getEmail(), loginMethod, ipAddress);

            return buildLoginResponse(accessToken, refreshToken, user, roles);

        } catch (AppException e) {
            String email = request.getEmail() != null ? request.getEmail() : request.getPhoneNumber();
            String reason = determineFailureReason(e.getErrorCode());

            auditLogger.logFailedLogin(email, ipAddress, userAgent, reason, loginMethod);

            log.warn("Login failed: email/phone={}, reason={}, ip={}",
                    email, reason, ipAddress);

            throw e;
        }
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
//                .fullName(request.getFullName())
                .phone(request.getPhoneNumber())
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(newUser);
        createUserProfile(savedUser.getId(), request.getFullName());

        assignDefaultRole(savedUser.getId());

        log.info("New user registered successfully: {}", savedUser.getEmail());

        List<String> roles = List.of("USER");
        return buildUserResponse(savedUser, roles);
    }

    @Override
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {

        String refreshTokenStr = request.getRefreshToken();

        try {
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

            // Generate new tokens
            List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
            UserProfile profile = getUserProfile(user.getId()).orElse(null);
            String newAccessToken = jwtTokenGenerator.generateAccessToken(user, roles, getFullName(profile));
            String newRefreshToken = jwtTokenGenerator.generateRefreshToken(user.getId());

            auditLogger.logTokenRefresh(userId, user.getEmail(), null, null, true);

            log.info("Token refreshed successfully: userId={}", userId);

            return buildLoginResponse(newAccessToken, newRefreshToken, user, roles);

        } catch (Exception e) {
            try {
                UUID userId = jwtTokenValidator.getUserIdFromToken(refreshTokenStr);
                User user = findUserById(userId);
                auditLogger.logTokenRefresh(userId, user.getEmail(), null, null, false);
            } catch (Exception ex) {
                log.error("Could not log failed token refresh", ex);
            }

            throw e;
        }
    }

    @Override
    public void logout(String refreshTokenStr, UUID userId, String ipAddress, String userAgent) {
        try {
            User user = findUserById(userId);

            // Invalidate token
            invalidateToken(refreshTokenStr, userId, "USER_LOGOUT", ipAddress, userAgent);

            // Log successful logout
            auditLogger.logLogout(userId, user.getEmail(), ipAddress, userAgent);

            log.info("User logged out successfully: userId={}, email={}, ip={}",
                    userId, user.getEmail(), ipAddress);

        } catch (Exception e) {
            log.error("Error during logout for userId={}", userId, e);
            throw e;
        }
    }

    @Override
    public UserResponse getCurrentUser(UUID userId) {
        User user = findUserById(userId);
        List<String> roles = userRoleRepository.findRoleNamesByUserId(userId);
        return buildUserResponse(user, roles);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(INVALID_CREDENTIALS));
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new AppException(ACCOUNT_DISABLED);
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }
    }

    private void validateAuthProvider(User user) {
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new AppException(
                    WRONG_AUTH_PROVIDER,
                    "Tài khoản này đăng nhập bằng " + user.getAuthProvider().name()
            );
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new AppException(INVALID_CREDENTIALS);
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
        UserProfile profile = getUserProfile(user.getId()).orElse(null);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(getFullName(profile))
                .phoneNumber(user.getPhone())
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .status(user.getStatus().name())
                .roles(roles)
                .build();
    }

    private Optional<UserProfile> getUserProfile(UUID userId) {
        return userProfileRepository.findByUserId(userId);
    }

    private String getFullName(UserProfile profile) {
        return profile != null ? profile.getFullName() : null;
    }

    private void createUserProfile(UUID userId, String fullName) {
        if (userProfileRepository.existsByUserId(userId)) {
            return;
        }

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .fullName(fullName)
                .avatarUrl(null)
                .reputationScore(50)
                .totalReportsSubmitted(0)
                .build();

        userProfileRepository.save(profile);
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

    private String determineFailureReason(BaseErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_CREDENTIALS -> "INVALID_CREDENTIALS";
            case ACCOUNT_DISABLED -> "ACCOUNT_DISABLED";
            case ACCOUNT_BANNED -> "ACCOUNT_BANNED";
            case WRONG_AUTH_PROVIDER -> "WRONG_AUTH_PROVIDER";
            default -> "UNKNOWN_ERROR";
        };
    }
}
