package org.project.floodalert.notification.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;
import org.project.floodalert.notification.enums.NotificationChannel;
import org.project.floodalert.notification.enums.NotificationPriority;
import org.project.floodalert.notification.enums.NotificationStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "notifications"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "title", nullable = false, length = 255)
    String title;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    String body;

    @Column(name = "notification_type", length = 50)
    String notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    @Builder.Default
    NotificationPriority priority = NotificationPriority.NORMAL;

    @Type(JsonBinaryType.class)
    @Column(name = "data", columnDefinition = "jsonb")
    Map<String, Object> data;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    @Builder.Default
    NotificationChannel channel = NotificationChannel.PUSH;

    @Column(name = "fcm_token", columnDefinition = "text")
    String fcmToken;

    @Column(name = "fcm_message_id", length = 255)
    String fcmMessageId;

    @Column(name = "email_to", length = 255)
    String emailTo;

    @Column(name = "email_subject", length = 500)
    String emailSubject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "text")
    String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    Integer maxRetries = 3;

    @Column(name = "next_retry_at")
    LocalDateTime nextRetryAt;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "sent_at")
    LocalDateTime sentAt;

    @Column(name = "delivered_at")
    LocalDateTime deliveredAt;

    @Column(name = "clicked_at")
    LocalDateTime clickedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
