package org.project.floodalert.auth.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.config.JwtProperties;
import org.project.floodalert.auth.constants.RoleConstants;
import org.project.floodalert.auth.dto.request.SocialLoginRequest;
import org.project.floodalert.auth.dto.response.FirebaseUserInfo;
import org.project.floodalert.auth.dto.response.LoginResponse;
import org.project.floodalert.auth.dto.response.UserResponse;
import org.project.floodalert.auth.enums.AuthProvider;
import org.project.floodalert.auth.enums.UserStatus;
import org.project.floodalert.auth.model.User;
import org.project.floodalert.auth.model.UserProfile;
import org.project.floodalert.auth.repository.UserProfileRepository;
import org.project.floodalert.auth.repository.UserRepository;
import org.project.floodalert.auth.repository.UserRoleRepository;
import org.project.floodalert.auth.security.JwtTokenGenerator;
import org.project.floodalert.auth.service.FirebaseAuthService;
import org.project.floodalert.auth.service.RoleService;
import org.project.floodalert.auth.service.SocialAuthService;
import org.project.floodalert.auth.utils.AuditLogger;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocialAuthServiceImpl implements SocialAuthService {

    FirebaseAuthService firebaseAuthService;
    UserRepository userRepository;
    UserProfileRepository userProfileRepository;
    UserRoleRepository userRoleRepository;
    JwtTokenGenerator jwtTokenGenerator;
    JwtProperties jwtProperties;
    RoleService roleService;
    AuditLogger auditLogger;

    @Override
    @Transactional
    public LoginResponse authenticateWithSocial(SocialLoginRequest request, String ipAddress, String userAgent) {

        FirebaseUserInfo firebaseUserInfo = firebaseAuthService.verifyIdToken(request.getIdToken());

        if (request.getProvider() != null) {
            firebaseAuthService.validateProvider(request.getProvider(), firebaseUserInfo.getProvider());
        }

        User user = syncUserWithDatabase(firebaseUserInfo);

        validateUserStatus(user);

        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        UserProfile profile = getOrCreateProfile(user.getId(), firebaseUserInfo.getFullName(), firebaseUserInfo.getAvatarUrl());
        String accessToken = jwtTokenGenerator.generateAccessToken(user, roles, profile.getFullName());
        String refreshToken = jwtTokenGenerator.generateRefreshToken(user.getId());
        updateLastLogin(user);
        auditLogger.logLogin(
                user.getId(),
                user.getEmail(),
                ipAddress,
                userAgent,
                firebaseUserInfo.getProvider() + "_FIREBASE"
        );

        log.info("Social authentication successful: email={}, provider={}, userId={}",
                user.getEmail(), firebaseUserInfo.getProvider(), user.getId());

        return buildLoginResponse(accessToken, refreshToken, user, roles);

    }


    private User syncUserWithDatabase(FirebaseUserInfo firebaseUserInfo) {
        Optional<User> existingUserByUid = userRepository.findByFirebaseUid(firebaseUserInfo.getFirebaseUid());

        if(existingUserByUid.isPresent()) {
            //User exists with firebase UID, update info
            return updateExistingUser(existingUserByUid.get(), firebaseUserInfo);
        }

        Optional<User> existingUserByEmail = userRepository.findByEmail(firebaseUserInfo.getEmail());
        if(existingUserByEmail.isPresent()) {
            User user = existingUserByEmail.get();
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                log.info("Linking Firebase account to existing local account: email={}", user.getEmail());
            }

            return updateExistingUser(user, firebaseUserInfo);
        }

        return createNewUser(firebaseUserInfo);
    }

    private User createNewUser(FirebaseUserInfo firebaseUserInfo) {
        AuthProvider authProvider = mapFirebaseProviderToAuthProvider(firebaseUserInfo.getProvider());

        User newUser = User.builder()
                .firebaseUid(firebaseUserInfo.getFirebaseUid())
                .email(firebaseUserInfo.getEmail())
                .authProvider(authProvider)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(newUser);
        getOrCreateProfile(savedUser.getId(), firebaseUserInfo.getFullName(), firebaseUserInfo.getAvatarUrl());
        assignDefaultRole(savedUser.getId());

        auditLogger.logUserRegistration(
                savedUser.getId(),
                savedUser.getEmail(),
                null, // IP address not available in this context
                null  // User agent not available in this context
        );

        return savedUser;
    }

    private User updateExistingUser(User user, FirebaseUserInfo firebaseUserInfo) {
        boolean updated = false;
        if(user.getFirebaseUid() == null || !user.getFirebaseUid().equals(firebaseUserInfo.getFirebaseUid())) {
            user.setFirebaseUid(firebaseUserInfo.getFirebaseUid());
            updated = true;
        }
        AuthProvider newProvider = mapFirebaseProviderToAuthProvider(firebaseUserInfo.getProvider());
        if(user.getAuthProvider() != newProvider) {
            user.setAuthProvider(newProvider);
            updated = true;
        }
        if(firebaseUserInfo.isEmailVerified() && (user.getEmailVerified() == null)){
            user.setEmailVerified(true);
            updated = true;
        }

        if (updated) {
            user.setUpdatedAt(LocalDateTime.now());
            user = userRepository.save(user);
            log.info("User updated successfully: userId={}", user.getId());
        }

        updateUserProfile(user.getId(), firebaseUserInfo.getFullName(), firebaseUserInfo.getAvatarUrl());

        return user;

    }

    private AuthProvider mapFirebaseProviderToAuthProvider(String provider) {
        return switch (provider.toLowerCase()){
            case "google" -> AuthProvider.GOOGLE;
            case "facebook" -> AuthProvider.FACEBOOK;
            default -> AuthProvider.LOCAL;
        };
    }

    private LoginResponse buildLoginResponse(String accessToken, String refreshToken, User user, List<String> roles) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(profile != null ? profile.getFullName() : null)
                .phoneNumber(user.getPhone())
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .status(user.getStatus().name())
                .roles(roles)
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration())
                .userResponse(userResponse)
                .build();
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED,
                    "Tài khoản đã bị vô hiệu hóa");
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED,
                    "Tài khoản đã bị cấm");
        }
    }

    private void updateLastLogin(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private void assignDefaultRole(java.util.UUID userId) {
        try {
            roleService.assignRoleToUser(userId, RoleConstants.USER);
            log.info("Default role assigned to user: userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to assign default role to user: userId={}", userId, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Không thể gán quyền cho người dùng");
        }
    }

    private UserProfile getOrCreateProfile(UUID userId, String fullName, String avatarUrl) {
        return userProfileRepository.findByUserId(userId)
                .map(profile -> updateUserProfile(profile, fullName, avatarUrl))
                .orElseGet(() -> userProfileRepository.save(UserProfile.builder()
                        .userId(userId)
                        .fullName(fullName)
                        .avatarUrl(avatarUrl)
                        .reputationScore(50)
                        .totalReportsSubmitted(0)
                        .build()));
    }

    private void updateUserProfile(UUID userId, String fullName, String avatarUrl) {
        userProfileRepository.findByUserId(userId)
                .ifPresent(profile -> updateUserProfile(profile, fullName, avatarUrl));
    }

    private UserProfile updateUserProfile(UserProfile profile, String fullName, String avatarUrl) {
        boolean changed = false;

        if (fullName != null && !fullName.equals(profile.getFullName())) {
            profile.setFullName(fullName);
            changed = true;
        }
        if (avatarUrl != null && !avatarUrl.equals(profile.getAvatarUrl())) {
            profile.setAvatarUrl(avatarUrl);
            changed = true;
        }

        return changed ? userProfileRepository.save(profile) : profile;
    }
}
