package org.project.floodalert.notification.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notification_preferences")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationPreference {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    UUID userId;

    @Column(name = "enabled")
    @Builder.Default
    Boolean enabled = true;

    @Column(name = "flood_alerts")
    @Builder.Default
    Boolean floodAlerts = true;

    @Column(name = "flood_updates")
    @Builder.Default
    Boolean floodUpdates = true;

    @Column(name = "fcm_token")
    String fcmToken;

    @Column(name = "system_updates")
    @Builder.Default
    Boolean systemUpdates = true;

    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    Boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    LocalTime quietHoursEnd;

    @Column(name = "alert_radius_meters")
    @Builder.Default
    Integer alertRadiusMeters = 500;

    @Column(name = "prefer_push")
    @Builder.Default
    Boolean preferPush = true;

    @Column(name = "prefer_email")
    @Builder.Default
    Boolean preferEmail = false;

    @Column(name = "prefer_sms")
    @Builder.Default
    Boolean preferSms = false;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
}
