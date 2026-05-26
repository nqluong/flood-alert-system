package org.project.floodalert.floodprocessor.service.scoring.impl;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.response.ScoringResult;
import org.project.floodalert.floodprocessor.service.scoring.ReportScoringStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class ReportScoringEngine {

    private final List<ReportScoringStrategy> strategies;
    private final ExecutorService virtualThreadExecutor;

    public ReportScoringEngine(List<ReportScoringStrategy> strategies,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor) {
        this.strategies = strategies;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    public ScoringResult evaluateScore(ReportMessage msg) {
        boolean hasImage = msg.getImageUrl() != null && !msg.getImageUrl().isEmpty();

        List<ReportScoringStrategy> applicableStrategies = strategies.stream()
                .filter(s -> s.isApplicable(msg))
                .toList();

        log.info("[SCORING-ENGINE] Bắt đầu chấm điểm song song cho reportId={}, userId={} — {} tiêu chí (hasImage={})",
                msg.getReportId(), msg.getUserId(), applicableStrategies.size(), hasImage);

        long startTime = System.currentTimeMillis();

        record FutureWithName(String name, CompletableFuture<Double> future) {
        }

        List<FutureWithName> futures = applicableStrategies.stream()
                .map(s -> new FutureWithName(
                        s.getStrategyName(),
                        CompletableFuture.supplyAsync(() -> s.calculateScore(msg), virtualThreadExecutor)))
                .toList();

        Map<String, Double> successfulRawScores = new HashMap<>();
        Set<String> failedNames = new HashSet<>();

        for (FutureWithName fwn : futures) {
            try {
                successfulRawScores.put(fwn.name(), fwn.future().join());
            } catch (Exception e) {
                failedNames.add(fwn.name());
                log.error("[SCORING-ENGINE] Strategy={} thất bại cho reportId={}: {}",
                        fwn.name(), msg.getReportId(), e.getMessage());
            }
        }

        double successfulWeightSum = applicableStrategies.stream()
                .filter(s -> !failedNames.contains(s.getStrategyName()))
                .mapToDouble(s -> getDynamicWeight(s.getStrategyName(), hasImage))
                .sum();

        if (successfulWeightSum == 0) {
            log.warn("[SCORING-ENGINE] Tất cả strategy thất bại cho reportId={}", msg.getReportId());
            return ScoringResult.builder().totalScore(0.0).build();
        }

        if (!failedNames.isEmpty()) {
            log.warn("[SCORING-ENGINE] reportId={} — Strategy thất bại: {}. Phân phối lại trọng số trên tổng={}",
                    msg.getReportId(), failedNames, successfulWeightSum);
        }

        double totalScore = 0.0;
        double aiScore = 0.0;
        double spatialScore = 0.0;
        double reputationScore = 0.0;

        for (Map.Entry<String, Double> entry : successfulRawScores.entrySet()) {
            String name = entry.getKey();
            double rawScore = entry.getValue();
            double normalizedWeight = getDynamicWeight(name, hasImage) / successfulWeightSum;
            double weightedScore = rawScore * normalizedWeight;

            log.info("[SCORING-ENGINE] Strategy={} → rawScore={}, normalizedWeight={}, weightedScore={}",
                    name, rawScore, normalizedWeight, weightedScore);

            totalScore += weightedScore;
            switch (name) {
                case "AI_VISION" -> aiScore = rawScore;
                case "SPATIAL_CONSENSUS" -> spatialScore = rawScore;
                case "USER_REPUTATION" -> reputationScore = rawScore;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[SCORING-ENGINE] reportId={} — Tổng điểm: {}, thời gian xử lý: {}ms",
                msg.getReportId(), totalScore, elapsed);

        return ScoringResult.builder()
                .totalScore(totalScore)
                .aiScore(aiScore)
                .spatialScore(spatialScore)
                .reputationScore(reputationScore)
                .build();
    }

    @Deprecated
    public double evaluateTotalScore(ReportMessage msg) {
        return evaluateScore(msg).getTotalScore();
    }

    private double getDynamicWeight(String strategyName, boolean hasImage) {
        if (hasImage) {
            return switch (strategyName) {
                case "AI_VISION" -> 0.50;
                case "SPATIAL_CONSENSUS" -> 0.30;
                case "USER_REPUTATION" -> 0.20;
                default -> 0.0;
            };
        } else {
            return switch (strategyName) {
                case "SPATIAL_CONSENSUS" -> 0.60;
                case "USER_REPUTATION" -> 0.40;
                default -> 0.0;
            };
        }
    }

}
