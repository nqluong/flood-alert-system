package org.project.floodalert.floodprocessor.service.scoring;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.response.ScoringResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Engine chấm điểm báo cáo sử dụng nhiều strategy song song trên virtual threads.
 * Trả về ScoringResult chứa tổng điểm và các điểm thành phần.
 */
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

    /**
     * Chấm điểm báo cáo sử dụng tất cả các strategy áp dụng được.
     *
     * @param msg Thông tin báo cáo
     * @return ScoringResult chứa tổng điểm và các điểm thành phần
     */
    public ScoringResult evaluateScore(ReportMessage msg) {
        boolean hasImage = msg.getImageUrl() != null && !msg.getImageUrl().isEmpty();

        // Lọc các strategy áp dụng được
        List<ReportScoringStrategy> applicableStrategies = strategies.stream()
                .filter(s -> s.isApplicable(msg))
                .toList();

        log.info("[SCORING-ENGINE] Bắt đầu chấm điểm song song cho reportId={}, userId={} — {} tiêu chí (hasImage={})",
                msg.getReportId(), msg.getUserId(), applicableStrategies.size(), hasImage);

        long startTime = System.currentTimeMillis();

        // Map để lưu trữ điểm theo strategy name
        Map<String, Double> strategyScores = new HashMap<>();

        List<CompletableFuture<StrategyScore>> futures = applicableStrategies.stream()
                .map(strategy -> CompletableFuture.supplyAsync(() -> {
                    double rawScore = strategy.calculateScore(msg);
                    double weight = getDynamicWeight(strategy.getStrategyName(), hasImage);
                    double weightedScore = rawScore * weight;
                    log.info("[SCORING-ENGINE] Strategy={} → rawScore={}, weight={}, weightedScore={}",
                            strategy.getStrategyName(), rawScore, weight, weightedScore);
                    return new StrategyScore(strategy.getStrategyName(), rawScore, weightedScore);
                }, virtualThreadExecutor))
                .toList();

        double totalScore = 0.0;
        double aiScore = 0.0;
        double spatialScore = 0.0;
        double reputationScore = 0.0;

        for (CompletableFuture<StrategyScore> future : futures) {
            try {
                StrategyScore result = future.join();
                totalScore += result.weightedScore;

                // Lưu raw score theo loại strategy (không phải weighted)
                switch (result.strategyName) {
                    case "AI_VISION" -> aiScore = result.rawScore;
                    case "SPATIAL_CONSENSUS" -> spatialScore = result.rawScore;
                    case "USER_REPUTATION" -> reputationScore = result.rawScore;
                }
            } catch (Exception e) {
                log.error("[SCORING-ENGINE] Strategy thất bại cho reportId={}: {}",
                        msg.getReportId(), e.getMessage(), e);
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

    /**
     * Backward compatibility: Trả về tổng điểm dạng double.
     *
     * @deprecated Sử dụng evaluateScore() để lấy thông tin chi tiết
     */
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

    /**
     * Internal record để lưu kết quả từ mỗi strategy.
     */
    private record StrategyScore(String strategyName, double rawScore, double weightedScore) {
    }
}
