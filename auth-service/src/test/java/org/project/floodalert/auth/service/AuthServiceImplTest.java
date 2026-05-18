package org.project.floodalert.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.auth.config.JwtProperties;
import org.project.floodalert.auth.dto.request.LoginRequest;
import org.project.floodalert.auth.dto.request.RefreshTokenRequest;
import org.project.floodalert.auth.dto.request.RegisterRequest;
import org.project.floodalert.auth.dto.response.LoginResponse;
import org.project.floodalert.auth.dto.response.UserResponse;
import org.project.floodalert.auth.enums.AuthProvider;
import org.project.floodalert.auth.enums.UserStatus;
import org.project.floodalert.auth.exception.AuthErrorCode;
import org.project.floodalert.auth.model.User;
import org.project.floodalert.auth.model.UserProfile;
import org.project.floodalert.auth.repository.InvalidatedTokenRepository;
import org.project.floodalert.auth.repository.UserProfileRepository;
import org.project.floodalert.auth.repository.UserRepository;
import org.project.floodalert.auth.repository.UserRoleRepository;
import org.project.floodalert.auth.security.JwtTokenGenerator;
import org.project.floodalert.auth.security.JwtTokenValidator;
import org.project.floodalert.auth.service.impl.AuthServiceImpl;
import org.project.floodalert.auth.utils.AuditLogger;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenGenerator jwtTokenGenerator;
    @Mock JwtTokenValidator jwtTokenValidator;
    @Mock JwtProperties jwtProperties;
    @Mock AuditLogger auditLogger;
    @Mock RoleService roleService;
    @Mock InvalidatedTokenRepository invalidatedTokenRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock ReputationCacheService reputationCacheService;

    @InjectMocks
    AuthServiceImpl authService;

    private UUID userId;
    private User activeLocalUser;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeLocalUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .phone("0901234567")
                .passwordHash("hashed_password")
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();

        userProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fullName("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .reputationScore(75)
                .build();
    }
    
    @Test
    void login_byEmail_withProfile_success() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com").password("Password1").build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeLocalUser));
        when(passwordEncoder.matches("Password1", "hashed_password")).thenReturn(true);
        when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("USER"));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(userProfile));
        when(jwtTokenGenerator.generateAccessToken(any(), any(), eq("Test User"))).thenReturn("access_token");
        when(jwtTokenGenerator.generateRefreshToken(userId)).thenReturn("refresh_token");
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600L);
        when(userRepository.save(any())).thenReturn(activeLocalUser);

        LoginResponse response = authService.login(request, "127.0.0.1", "Mozilla");

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        verify(reputationCacheService).cacheAsync(userId, 75);
        verify(auditLogger).logLogin(userId, "test@example.com", "127.0.0.1", "Mozilla", "EMAIL");
    }

    @Test
    void login_byEmail_withoutProfile_skipsReputationCache() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com").password("Password1").build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeLocalUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("USER"));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(jwtTokenGenerator.generateAccessToken(any(), any(), isNull())).thenReturn("access_token");
        when(jwtTokenGenerator.generateRefreshToken(any())).thenReturn("refresh_token");
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600L);
        when(userRepository.save(any())).thenReturn(activeLocalUser);

        LoginResponse response = authService.login(request, "127.0.0.1", "Mozilla");

        assertThat(response).isNotNull();
        verify(reputationCacheService, never()).cacheAsync(any(), anyInt());
    }

    @Test
    void login_byPhone_success() {
        LoginRequest request = LoginRequest.builder()
                .phoneNumber("0901234567").password("Password1").build();

        when(userRepository.findByPhone("0901234567")).thenReturn(Optional.of(activeLocalUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("USER"));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(jwtTokenGenerator.generateAccessToken(any(), any(), any())).thenReturn("access_token");
        when(jwtTokenGenerator.generateRefreshToken(any())).thenReturn("refresh_token");
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600L);
        when(userRepository.save(any())).thenReturn(activeLocalUser);

        LoginResponse response = authService.login(request, "10.0.0.1", "Chrome");

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        verify(auditLogger).logLogin(any(), any(), any(), any(), eq("PHONE"));
    }

    @Test
    void login_noCredentials_throwsInvalidCredentials() {
        LoginRequest request = LoginRequest.builder().build();

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "Mozilla"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(auditLogger).logFailedLogin(isNull(), any(), any(), eq("INVALID_CREDENTIALS"), isNull());
    }

    @Test
    void login_whitespaceCredentials_throwsInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("   ").phoneNumber("   ").build();

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "Mozilla"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void login_emailNotFound_throwsInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("missing@example.com").password("Password1").build();

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "Mozilla"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void login_phoneNotFound_throwsInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .phoneNumber("0000000000").password("Password1").build();

        when(userRepository.findByPhone("0000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "Mozilla"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void login_userDisabled_throwsAccountDisabled() {
        User disabledUser = User.builder()
                .id(userId).email("test@example.com").passwordHash("hashed_password")
                .status(UserStatus.DISABLED).authProvider(AuthProvider.LOCAL).build();

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> authService.login(emailRequest(), "127.0.0.1", "Mozilla"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_DISABLED));

        verify(auditLogger).logFailedLogin(any(), any(), any(), eq("ACCOUNT_DISABLED"), eq("EMAIL"));
    }

    @Test
    void login_userBanned_throwsAccountBanned() {
        User bannedUser = User.builder()
                .id(userId).email("test@example.com").passwordHash("hashed_password")
                .status(UserStatus.BANNED).authProvider(AuthProvider.LOCAL).build();

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(bannedUser));

        assertThatThrownBy(() -> authService.login(emailRequest(), "127.0.0.1", "Mozilla"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_BANNED));

        verify(auditLogger).logFailedLogin(any(), any(), any(), eq("ACCOUNT_BANNED"), eq("EMAIL"));
    }

    @Test
    void login_nonLocalAuthProvider_throwsWrongProvider() {
        User googleUser = User.builder()
                .id(userId).email("test@example.com").passwordHash("hashed_password")
                .status(UserStatus.ACTIVE).authProvider(AuthProvider.GOOGLE).build();

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(googleUser));

        assertThatThrownBy(() -> authService.login(emailRequest(), "127.0.0.1", "Mozilla"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.WRONG_AUTH_PROVIDER));

        verify(auditLogger).logFailedLogin(any(), any(), any(), eq("WRONG_AUTH_PROVIDER"), eq("EMAIL"));
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(activeLocalUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(emailRequest(), "127.0.0.1", "Mozilla"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(auditLogger).logFailedLogin(eq("test@example.com"), any(), any(), eq("INVALID_CREDENTIALS"), eq("EMAIL"));
    }


//    @Test
//    void register_withoutPhone_success() {
//        RegisterRequest request = RegisterRequest.builder()
//                .email("new@example.com").password("NewPass1").fullName("New User").build();
//
//        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
//        when(passwordEncoder.encode("NewPass1")).thenReturn("hashed_new");
//        when(userRepository.save(any())).thenReturn(activeLocalUser);
//        when(userProfileRepository.existsByUserId(any())).thenReturn(false);
//        when(userRoleRepository.findRoleNamesByUserId(any())).thenReturn(List.of("USER"));
//        when(userProfileRepository.findByUserId(any())).thenReturn(Optional.empty());
//
//        UserResponse response = authService.register(request, null);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getRoles()).containsExactly("USER");
//        verify(userProfileRepository).save(any());
//        verify(roleService).assignRoleToUser(any(), eq("USER"));
//    }
//
//    @Test
//    void register_withPhone_checksPhoneUniqueness() {
//        RegisterRequest request = RegisterRequest.builder()
//                .email("new@example.com").password("NewPass1")
//                .fullName("New User").phoneNumber("0901234567").build();
//
//        when(userRepository.existsByEmail(any())).thenReturn(false);
//        when(userRepository.existsByPhone("0901234567")).thenReturn(false);
//        when(passwordEncoder.encode(any())).thenReturn("hashed");
//        when(userRepository.save(any())).thenReturn(activeLocalUser);
//        when(userProfileRepository.existsByUserId(any())).thenReturn(false);
//        when(userRoleRepository.findRoleNamesByUserId(any())).thenReturn(List.of("USER"));
//        when(userProfileRepository.findByUserId(any())).thenReturn(Optional.empty());
//
//        authService.register(request, null);
//
//        verify(userRepository).existsByPhone("0901234567");
//    }

    @Test
    void register_emailAlreadyExists_throws() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com").password("NewPass1").fullName("User").build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, null))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AuthErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void register_phoneAlreadyExists_throws() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@example.com").password("NewPass1")
                .fullName("User").phoneNumber("0901234567").build();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByPhone("0901234567")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, null))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AuthErrorCode.PHONE_ALREADY_EXISTS));
    }

    @Test
    void register_roleAssignmentFails_throwsInternalServerError() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@example.com").password("NewPass1").fullName("User").build();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(activeLocalUser);
        when(userProfileRepository.existsByUserId(any())).thenReturn(false);
        doThrow(new RuntimeException("DB error")).when(roleService).assignRoleToUser(any(), any());

        assertThatThrownBy(() -> authService.register(request, null))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
    }

//    @Test
//    void register_profileAlreadyExists_skipsProfileSave() {
//        RegisterRequest request = RegisterRequest.builder()
//                .email("new@example.com").password("NewPass1").fullName("User").build();
//
//        when(userRepository.existsByEmail(any())).thenReturn(false);
//        when(passwordEncoder.encode(any())).thenReturn("hashed");
//        when(userRepository.save(any())).thenReturn(activeLocalUser);
//        when(userProfileRepository.existsByUserId(any())).thenReturn(true);
//        when(userRoleRepository.findRoleNamesByUserId(any())).thenReturn(List.of("USER"));
//        when(userProfileRepository.findByUserId(any())).thenReturn(Optional.empty());
//
//        authService.register(request, null);
//
//        verify(userProfileRepository, never()).save(any());
//    }

    @Test
    void refreshAccessToken_success() {
        String token = "valid.refresh.token";
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(token).build();

        when(jwtTokenValidator.validateToken(token)).thenReturn(true);
        when(jwtTokenValidator.getTokenType(token)).thenReturn("refresh_token");
        when(jwtTokenValidator.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeLocalUser));
        when(jwtTokenValidator.getJtiFromToken(token)).thenReturn("jti-abc");
        when(jwtTokenValidator.getExpirationFromToken(token)).thenReturn(LocalDateTime.now().plusHours(1));
        when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("USER"));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(userProfile));
        when(jwtTokenGenerator.generateAccessToken(any(), any(), any())).thenReturn("new_access");
        when(jwtTokenGenerator.generateRefreshToken(any())).thenReturn("new_refresh");
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600L);

        LoginResponse response = authService.refreshAccessToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new_access");
        assertThat(response.getRefreshToken()).isEqualTo("new_refresh");
        verify(invalidatedTokenRepository).save(any());
        verify(auditLogger).logTokenRefresh(userId, "test@example.com", null, null, true);
    }

    @Test
    void refreshAccessToken_invalidToken_logsFailedAuditAndThrows() {
        String token = "bad.token";
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(token).build();

        when(jwtTokenValidator.validateToken(token)).thenReturn(false);
        when(jwtTokenValidator.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeLocalUser));

        assertThatThrownBy(() -> authService.refreshAccessToken(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TOKEN));

        verify(auditLogger).logTokenRefresh(userId, "test@example.com", null, null, false);
    }

    @Test
    void refreshAccessToken_invalidToken_getUserIdFails_swallowsInnerError() {
        String token = "unparseable.token";
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(token).build();

        when(jwtTokenValidator.validateToken(token)).thenReturn(false);
        when(jwtTokenValidator.getUserIdFromToken(token)).thenThrow(new RuntimeException("parse error"));

        assertThatThrownBy(() -> authService.refreshAccessToken(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TOKEN));

        verify(auditLogger, never()).logTokenRefresh(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void refreshAccessToken_wrongTokenType_throws() {
        String token = "access.token.used.as.refresh";
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(token).build();

        when(jwtTokenValidator.validateToken(token)).thenReturn(true);
        when(jwtTokenValidator.getTokenType(token)).thenReturn("access_token");
        when(jwtTokenValidator.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeLocalUser));

        assertThatThrownBy(() -> authService.refreshAccessToken(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    void refreshAccessToken_userNotFound_throws() {
        String token = "valid.token";
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(token).build();

        when(jwtTokenValidator.validateToken(token)).thenReturn(true);
        when(jwtTokenValidator.getTokenType(token)).thenReturn("refresh_token");
        when(jwtTokenValidator.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshAccessToken(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void refreshAccessToken_invalidateTokenThrows_continuesAndReturnsTokens() {
        String token = "valid.refresh.token";
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(token).build();

        when(jwtTokenValidator.validateToken(token)).thenReturn(true);
        when(jwtTokenValidator.getTokenType(token)).thenReturn("refresh_token");
        when(jwtTokenValidator.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeLocalUser));
        when(jwtTokenValidator.getJtiFromToken(token)).thenThrow(new RuntimeException("JTI error"));
        when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("USER"));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(jwtTokenGenerator.generateAccessToken(any(), any(), any())).thenReturn("new_access");
        when(jwtTokenGenerator.generateRefreshToken(any())).thenReturn("new_refresh");
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600L);

        LoginResponse response = authService.refreshAccessToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new_access");
        verify(invalidatedTokenRepository, never()).save(any());
    }

    @Test
    void logout_success() {
        String token = "refresh.token";

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeLocalUser));
        when(jwtTokenValidator.getJtiFromToken(token)).thenReturn("jti-xyz");
        when(jwtTokenValidator.getExpirationFromToken(token)).thenReturn(LocalDateTime.now().plusHours(1));
        when(jwtTokenValidator.getTokenType(token)).thenReturn("refresh_token");

        authService.logout(token, userId, "127.0.0.1", "Mozilla");

        verify(invalidatedTokenRepository).save(any());
        verify(auditLogger).logLogout(userId, "test@example.com", "127.0.0.1", "Mozilla");
    }

    @Test
    void logout_withAccessTokenType_storesAsAccessType() {
        String token = "access.token";

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeLocalUser));
        when(jwtTokenValidator.getJtiFromToken(token)).thenReturn("jti-abc");
        when(jwtTokenValidator.getExpirationFromToken(token)).thenReturn(LocalDateTime.now().plusHours(1));
        when(jwtTokenValidator.getTokenType(token)).thenReturn("access_token");

        authService.logout(token, userId, "127.0.0.1", "Mozilla");

        verify(invalidatedTokenRepository).save(any());
    }

    @Test
    void logout_userNotFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("token", userId, "127.0.0.1", "Mozilla"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void getCurrentUser_withProfile_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeLocalUser));
        when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("USER", "ADMIN"));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(userProfile));

        UserResponse response = authService.getCurrentUser(userId);

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFullName()).isEqualTo("Test User");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getRoles()).containsExactly("USER", "ADMIN");
    }

    @Test
    void getCurrentUser_withoutProfile_returnsNullNameAndAvatar() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeLocalUser));
        when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("USER"));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UserResponse response = authService.getCurrentUser(userId);

        assertThat(response.getFullName()).isNull();
        assertThat(response.getAvatarUrl()).isNull();
    }

    @Test
    void getCurrentUser_userNotFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(userId))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    private LoginRequest emailRequest() {
        return LoginRequest.builder()
                .email("test@example.com").password("Password1").build();
    }
}
