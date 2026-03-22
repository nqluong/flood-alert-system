package org.project.floodalert.floodprocessor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả tổng hợp từ ReportScoringEngine.
 * Chứa tổng điểm và các điểm thành phần từ các strategy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringResult {

    /**
     * Tổng điểm tổng hợp (0.0 - 1.0)
     */
    private double totalScore;

    /**
     * Điểm từ AI Vision Strategy (0.0 - 1.0)
     */
    private double aiScore;

    /**
     * Điểm từ Spatial Consensus Strategy (0.0 - 1.0)
     */
    private double spatialScore;

    /**
     * Điểm từ User Reputation Strategy (0.0 - 1.0)
     */
    private double reputationScore;
}
