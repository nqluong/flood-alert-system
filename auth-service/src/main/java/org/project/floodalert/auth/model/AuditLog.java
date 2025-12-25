package org.project.floodalert.auth.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_logs")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "user_id")
    UUID userId;

    @Column(nullable = false)
    String action;

    @Column(name = "resource_type")
    String resourceType;

    @Column(name = "resource_id")
    UUID resourceId;

    @Column(name = "ip_address", columnDefinition = "inet")
    InetAddress ipAddress;

    @Column(name = "user_agent")
    String userAgent;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    Map<String, Object> metadata;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
