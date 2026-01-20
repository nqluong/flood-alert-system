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
    UUID ID;

    @Column(name = "flood_event_id", nullable = false)
    UUID FLOOD_EVENT_ID;

    @Column(name = "iot_score", precision = 3, scale = 2)
    BigDecimal IOT_SCORE;

    @Column(name = "user_vote_score", precision = 3, scale = 2)
    BigDecimal USER_VOTE_SCORE;

    @Column(name = "historical_score", precision = 3, scale = 2)
    BigDecimal HISTORICAL_SCORE;

    @Column(name = "spatial_correlation_score", precision = 3, scale = 2)
    BigDecimal SPATIAL_CORRELATION_SCORE;

    @Column(name = "final_score", nullable = false, precision = 3, scale = 2)
    BigDecimal FINAL_SCORE;

    @Column(name = "calculation_method", length = 50)
    String CALCULATION_METHOD;

    @Column(name = "algorithm_version", length = 20)
    String ALGORITHM_VERSION;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "factors", columnDefinition = "jsonb")
    String FACTORS;

    @Column(name = "calculated_at")
    LocalDateTime CALCULATED_AT;

    @PrePersist
    protected void onCreate() {
        CALCULATED_AT = LocalDateTime.now();
    }
}
