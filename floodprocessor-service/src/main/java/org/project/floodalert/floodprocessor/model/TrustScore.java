package org.project.floodalert.floodprocessor.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "trust_scores")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TrustScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "flood_event_id", nullable = false)
    UUID floodEventId;

    @Column(name = "iot_score", precision = 3, scale = 2)
    BigDecimal iotScore;

    @Column(name = "user_vote_score", precision = 3, scale = 2)
    BigDecimal userVoteScore;

    @Column(name = "historical_score", precision = 3, scale = 2)
    BigDecimal historicalScore;

    @Column(name = "spatial_correlation_score", precision = 3, scale = 2)
    BigDecimal spatialCorrelationScore;

    @Column(name = "final_score", nullable = false, precision = 3, scale = 2)
    BigDecimal finalScore;

    @Column(name = "calculation_method", length = 50)
    String calculationMethod;

    @Column(name = "algorithm_version", length = 20)
    String algorithmVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "factors", columnDefinition = "jsonb")
    String factors;

    @Column(name = "calculated_at")
    LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }
}
