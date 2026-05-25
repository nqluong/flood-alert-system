package org.project.floodalert.floodcore.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    UUID id;

    @Column(name = "report_id", nullable = false, unique = true, length = 50)
    String reportId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    String imageUrls;

    @Column(name = "severity_level", length = 20)
    String severityLevel;

    @Column(name = "lat", nullable = false, precision = 10, scale = 8)
    BigDecimal lat;

    @Column(name = "lon", nullable = false, precision = 11, scale = 8)
    BigDecimal lon;

    @Column(name = "flood_event_id", length = 50)
    String floodEventId;

    @Column(name = "status", length = 20)
    String status = "PENDING";

    @Column(name = "reviewed_by")
    UUID reviewedBy;

    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    String rejectReason;

    @Column(name = "score")
    Double score;

    @Column(name = "ai_score")
    Double aiScore;

    @Column(name = "spatial_score")
    Double spatialScore;

    @Column(name = "reputation_score")
    Double reputationScore;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
