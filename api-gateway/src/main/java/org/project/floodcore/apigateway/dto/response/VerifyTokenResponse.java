package org.project.floodcore.apigateway.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyTokenResponse {
    boolean valid;
    UUID userId;
    String email;
    List<String> roles;
    String tokenType;
    Long expiresAt;

    String invalidReason;

    public static VerifyTokenResponse invalid(String reason) {
        return VerifyTokenResponse.builder()
                .valid(false)
                .invalidReason(reason)
                .build();
    }

    public static VerifyTokenResponse valid(UUID userId, String email,
                                            List<String> roles, String tokenType,
                                            Long expiresAt) {
        return VerifyTokenResponse.builder()
                .valid(true)
                .userId(userId)
                .email(email)
                .roles(roles)
                .tokenType(tokenType)
                .expiresAt(expiresAt)
                .build();
    }
}