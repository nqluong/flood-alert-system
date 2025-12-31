package org.project.floodalert.auth.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.dto.response.FirebaseUserInfo;
import org.project.floodalert.auth.exception.AuthErrorCode;
import org.project.floodalert.auth.service.FirebaseAuthService;
import org.project.floodalert.common.exception.AppException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseAuthServiceImpl implements FirebaseAuthService {

    @Override
    public FirebaseUserInfo verifyIdToken(String idToken) {
        try {
            // Verify the ID token with Firebase
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

            // Extract user information from token
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String picture = decodedToken.getPicture();
            Boolean emailVerified = decodedToken.isEmailVerified();

            // Get provider information
            String provider = determineProvider(decodedToken);

            log.info("Firebase token verified successfully: uid={}, email={}, provider={}",
                    uid, email, provider);

            return FirebaseUserInfo.builder()
                    .firebaseUid(uid)
                    .email(email)
                    .fullName(name)
                    .avatarUrl(picture)
                    .emailVerified(emailVerified != null ? emailVerified : false)
                    .provider(provider)
                    .build();

        } catch (FirebaseAuthException e) {
            log.error("Firebase token verification failed: {}", e.getMessage(), e);

            // Handle specific Firebase errors
            String errorCode = e.getErrorCode().toString();
            throw switch (errorCode) {
                case "auth/id-token-expired" ->
                        new AppException(AuthErrorCode.TOKEN_EXPIRED, "Firebase token đã hết hạn");
                case "auth/id-token-revoked" ->
                        new AppException(AuthErrorCode.TOKEN_REVOKED, "Firebase token đã bị thu hồi");
                case "auth/invalid-id-token" ->
                        new AppException(AuthErrorCode.INVALID_TOKEN, "Firebase token không hợp lệ");
                case "auth/user-disabled" ->
                        new AppException(AuthErrorCode.ACCOUNT_DISABLED, "Tài khoản đã bị vô hiệu hóa");
                default ->
                        new AppException(AuthErrorCode.FIREBASE_AUTH_ERROR,
                                "Lỗi xác thực Firebase: " + e.getMessage());
            };
        } catch (Exception e) {
            log.error("Unexpected error during Firebase token verification", e);
            throw new AppException(AuthErrorCode.FIREBASE_AUTH_ERROR,
                    "Lỗi không xác định khi xác thực Firebase");
        }
    }

    private String determineProvider(FirebaseToken token) {
        // Firebase stores provider info in the sign_in_provider claim
        Object signInProvider = token.getClaims().get("firebase");

        if (signInProvider instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> firebaseMap = (java.util.Map<String, Object>) signInProvider;
            Object provider = firebaseMap.get("sign_in_provider");

            if (provider != null) {
                String providerStr = provider.toString();
                if (providerStr.contains("google")) {
                    return "GOOGLE";
                } else if (providerStr.contains("facebook")) {
                    return "FACEBOOK";
                }
            }
        }

        // Fallback: check email domain
        String email = token.getEmail();
        if (email != null && email.contains("@")) {
            return "GOOGLE"; // Default assumption
        }

        return "UNKNOWN";
    }


    @Override
    public void validateProvider(String expectedProvider, String actualProvider) {
        if (!expectedProvider.equalsIgnoreCase(actualProvider)) {
            throw new AppException(AuthErrorCode.WRONG_AUTH_PROVIDER,
                    String.format("Expected provider: %s, but got: %s", expectedProvider, actualProvider));
        }
    }
}
