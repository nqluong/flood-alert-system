package org.project.floodalert.auth.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.floodalert.auth.enums.TokenType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "invalidated_tokens")
public class InvalidatedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "token_jti", nullable = false, unique = true, length = 255)
    String tokenJti;

    @Column(name = "token_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    TokenType tokenType;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "invalidated_at")
    LocalDateTime invalidatedAt;

    @Column(name = "invalidated_reason", length = 50)
    String invalidatedReason;

    @Column(name = "ip_address", columnDefinition = "inet")
    String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    String userAgent;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (invalidatedAt == null) {
            invalidatedAt = LocalDateTime.now();
        }
    }
}
