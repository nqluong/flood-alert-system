package org.project.floodalert.floodprocessor.service.scoring.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.response.GeminiVisionResponse;
import org.project.floodalert.floodprocessor.service.scoring.GeminiApiClient;
import org.project.floodalert.floodprocessor.service.scoring.ReportScoringStrategy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiVisionScoringStrategy implements ReportScoringStrategy {

    private final GeminiApiClient geminiApiClient;

    @Override
    public double calculateScore(ReportMessage msg) {
        log.info("[SCORING][AI-VISION] Bắt đầu phân tích ảnh: reportId={}, imageUrl={}",
                msg.getReportId(), msg.getImageUrl());

        GeminiVisionResponse response = geminiApiClient.analyzeFloodImage(msg);

        if (response == null) {
            throw new IllegalStateException(
                    "Gemini API returned null response for reportId=" + msg.getReportId());
        }

        double normalizedScore = response.getConfidenceScore() / 100.0;

        log.info(
                "[SCORING][AI-VISION] reportId={} → isFlooded={}, waterLevel={}, confidenceScore={}, normalizedScore={}, reasoning='{}'",
                msg.getReportId(),
                response.isFlooded(),
                response.getWaterLevelEstimate(),
                response.getConfidenceScore(),
                normalizedScore,
                response.getReasoning());

        if (!response.isFlooded() && response.getConfidenceScore() < 30) {
            log.warn("[SCORING][AI-VISION] reportId={} - AI không chắc chắn (confidence < 30): {}",
                    msg.getReportId(), response.getReasoning());
        }

        return normalizedScore;
    }

    @Override
    public boolean isApplicable(ReportMessage msg) {
        return msg.getImageUrl() != null && !msg.getImageUrl().isBlank();
    }

    @Override
    public String getStrategyName() {
        return "AI_VISION";
    }
}
