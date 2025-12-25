package org.project.floodalert.auth.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.repository.InvalidatedTokenRepository;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenValidator {
    private final MACVerifier jwtVerifier;
    private final InvalidatedTokenRepository invalidatedTokenRepository;

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Verify signature
            if (!signedJWT.verify(jwtVerifier)) {
                log.error("Invalid JWT signature");
                return false;
            }

            // Check expiration
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                log.error("JWT token has expired");
                return false;
            }
            String jti = claims.getJWTID();
            if (jti != null && isTokenInvalidated(jti)) {
                log.error("JWT token has been invalidated (blacklisted)");
                return false;
            }

            return true;
        } catch (ParseException | JOSEException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }


    public boolean isTokenInvalidated(String jti) {
        return invalidatedTokenRepository.isTokenInvalidated(jti, LocalDateTime.now());
    }

    /**
     * Extract JWT ID (JTI) from token
     */
    public String getJtiFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            return claims.getJWTID();
        } catch (ParseException e) {
            log.error("Error parsing JWT token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Extract User ID from token
     */
    public UUID getUserIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            return UUID.fromString(claims.getSubject());
        } catch (ParseException e) {
            log.error("Error parsing JWT token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Extract Roles from token
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            return (List<String>) claims.getClaim("roles");
        } catch (ParseException e) {
            log.error("Error parsing JWT token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Extract Email from token
     */
    public String getEmailFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            return claims.getStringClaim("email");
        } catch (ParseException e) {
            log.error("Error parsing JWT token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Extract token type (access_token or refresh_token)
     */
    public String getTokenType(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            return claims.getStringClaim("type");
        } catch (ParseException e) {
            log.error("Error parsing JWT token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Get all claims from token
     */
    public JWTClaimsSet getClaimsFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            log.error("Error parsing JWT token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Get expiration time from token
     */
    public LocalDateTime getExpirationFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            return expiration != null ?
                    LocalDateTime.ofInstant(expiration.toInstant(),
                            java.time.ZoneId.systemDefault()) : null;
        } catch (ParseException e) {
            log.error("Error parsing JWT token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }
}
