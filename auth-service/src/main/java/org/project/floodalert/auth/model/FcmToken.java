package org.project.floodalert.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.project.floodalert.auth.enums.DeviceType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fcm_tokens")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "token", nullable = false, unique = true, columnDefinition = "text")
    String token;

    @Column(name = "device_type", length = 20)
    DeviceType deviceType;

    @Column(name = "device_id")
    String deviceId;

    @Column(name = "device_name")
    String deviceName;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_used_at")
    LocalDateTime lastUsedAt;
}
