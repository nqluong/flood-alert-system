package org.project.floodalert.auth.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.dto.response.VerifyTokenResponse;
import org.project.floodalert.auth.security.JwtTokenValidator;
import org.project.floodalert.auth.service.TokenVerificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenVerificationServiceImpl implements TokenVerificationService {
    JwtTokenValidator jwtTokenValidator;

    @Override
    public VerifyTokenResponse verifyToken(String token) {
        try {
            if (!jwtTokenValidator.validateToken(token)) {
                return VerifyTokenResponse.invalid("Token không hợp lệ hoặc đã hết hạn");
            }

            String tokenType = jwtTokenValidator.getTokenType(token);
            if (!"access_token".equals(tokenType)) {
                return VerifyTokenResponse.invalid("Token type không hợp lệ");
            }

            UUID userId = jwtTokenValidator.getUserIdFromToken(token);
            String email = jwtTokenValidator.getEmailFromToken(token);
            List<String> roles = jwtTokenValidator.getRolesFromToken(token);

            LocalDateTime expiration = jwtTokenValidator.getExpirationFromToken(token);
            Long expiresAt = expiration != null ?
                    expiration.atZone(ZoneId.systemDefault()).toEpochSecond() : null;

            return VerifyTokenResponse.valid(userId, email, roles, tokenType, expiresAt);

        } catch (Exception e) {
            log.error("Error verifying token: {}", e.getMessage());
            return VerifyTokenResponse.invalid("Lỗi khi xác thực token");
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            return jwtTokenValidator.validateToken(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
