package org.project.floodalert.floodcore.model;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_reports")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserReport {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "report_id", nullable = false, unique = true, length = 50)
    String reportId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Type(StringArrayType.class)
    @Column(name = "image_urls", columnDefinition = "text[]")
    String[] imageUrls;

    @Column(name = "severity_level", length = 20)
    String severityLevel;

    @Column(name = "lat", nullable = false, precision = 10, scale = 8)
    BigDecimal lat;

    @Column(name = "lon", nullable = false, precision = 11, scale = 8)
    BigDecimal lon;

    @Column(name = "status", length = 20)
    String status = "PENDING";

    @Column(name = "reviewed_by")
    UUID reviewedBy;

    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    String rejectReason;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
