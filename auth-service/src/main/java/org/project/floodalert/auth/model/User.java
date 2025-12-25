package org.project.floodalert.auth.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.floodalert.auth.enums.AuthProvider;
import org.project.floodalert.auth.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(unique = true, nullable = false)
    String email;

    @Column(name = "password_hash")
    String passwordHash;

    @Column(name = "full_name", nullable = false)
    String fullName;

    String phone;

    @Column(name = "avatar_url")
    String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    UserStatus status;

    @Column(name = "firebase_uid")
    String firebaseUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    AuthProvider authProvider;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
