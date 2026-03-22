package org.project.floodalert.floodprocessor.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.project.floodalert.floodprocessor.enums.SeverityLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeminiVisionResponse {

    @JsonProperty("is_flooded")
    boolean isFlooded;

    @JsonProperty("water_level_estimate")
    String waterLevelEstimate;

    /**
     * Điểm tin cậy do AI tự tính (thang 0-100).
     * Sẽ được chuẩn hoá về 0.0-1.0 tại {@code AiVisionScoringStrategy}.
     */
    @JsonProperty("confidence_score")
    int confidenceScore;

    @JsonProperty("reasoning")
    String reasoning;

    public SeverityLevel getMappedSeverityLevel() {
        return SeverityLevel.fromGeminiEstimate(this.waterLevelEstimate);
    }
}
