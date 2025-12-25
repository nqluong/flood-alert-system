package org.project.floodalert.auth.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.config.JwtProperties;
import org.project.floodalert.auth.model.User;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenGenerator {
    private final MACSigner jwtSigner;
    private final JWSHeader jwsHeader;
    private final JwtProperties jwtProperties;

    /**
     * Generate Access Token
     */
    public String generateAccessToken(User user, List<String> roles) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(jwtProperties.getAccessTokenExpiration());

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(jwtProperties.getIssuer())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiration))
                    .jwtID(UUID.randomUUID().toString())
                    .subject(user.getId().toString())
                    .claim("email", user.getEmail())
                    .claim("fullName", user.getFullName())
                    .claim("roles", roles)
                    .claim("type", "access_token")
                    .build();

            return createSignedToken(claims);
        } catch (JOSEException e) {
            log.error("Error generating access token", e);
            throw new RuntimeException("Error generating access token", e);
        }
    }

    /**
     * Generate Refresh Token
     */
    public String generateRefreshToken(UUID userId) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plus(jwtProperties.getRefreshTokenExpiration(), ChronoUnit.SECONDS);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(jwtProperties.getIssuer())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiration))
                    .jwtID(UUID.randomUUID().toString())
                    .subject(userId.toString())
                    .claim("type", "refresh_token")
                    .build();

            return createSignedToken(claims);
        } catch (JOSEException e) {
            log.error("Error generating refresh token", e);
            throw new RuntimeException("Error generating refresh token", e);
        }
    }


    private String createSignedToken(JWTClaimsSet claims) throws JOSEException {
        Payload payload = new Payload(claims.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        jwsObject.sign(jwtSigner);
        return jwsObject.serialize();
    }
}
