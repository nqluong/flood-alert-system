package org.project.floodalert.floodprocessor.service.scoring;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
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

    public double evaluateTotalScore(ReportMessage msg) {
        boolean hasImage = msg.getImageUrl() != null && !msg.getImageUrl().isEmpty();

        // Lọc các strategy áp dụng được dựa trên điều kiện (vd: AI_VISION chỉ chạy khi có ảnh)
        List<ReportScoringStrategy> applicableStrategies = strategies.stream()
                .filter(s -> s.isApplicable(msg))
                .toList();

        log.info("[SCORING-ENGINE] Bắt đầu chấm điểm song song cho reportId={}, userId={} — {} tiêu chí (hasImage={})",
                msg.getReportId(), msg.getUserId(), applicableStrategies.size(), hasImage);

        long startTime = System.currentTimeMillis();

        // Dispatch mỗi strategy áp dụng được lên một virtual thread riêng biệt
        List<CompletableFuture<Double>> futures = applicableStrategies.stream()
                .map(strategy -> CompletableFuture.supplyAsync(() -> {
                    double rawScore = strategy.calculateScore(msg);
                    double weight = getDynamicWeight(strategy.getStrategyName(), hasImage);
                    double weightedScore = rawScore * weight;
                    log.debug("[SCORING-ENGINE] Strategy={} → rawScore={}, weight={}, weightedScore={}",
                            strategy.getStrategyName(), rawScore, weight, weightedScore);
                    return weightedScore;
                }, virtualThreadExecutor))
                .toList();

        // Chờ tất cả strategy hoàn thành rồi cộng tổng
        double totalScore = futures.stream()
                .mapToDouble(future -> {
                    try {
                        return future.join();
                    } catch (Exception e) {
                        log.error("[SCORING-ENGINE] Strategy thất bại cho reportId={}: {}",
                                msg.getReportId(), e.getMessage(), e);
                        return 0.0; // strategy lỗi không làm hỏng toàn bộ kết quả
                    }
                })
                .sum();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[SCORING-ENGINE] reportId={} — Tổng điểm: {}, thời gian xử lý song song: {}ms",
                msg.getReportId(), totalScore, elapsed);

        return totalScore;
    }

    private double getDynamicWeight(String strategyName, boolean hasImage) {
        if(hasImage){
            return switch (strategyName){
                case "AI_VISION" -> 0.50;
                case "SPATIAL_CONSENSUS" -> 0.30;
                case "USER_REPUTATION" -> 0.20;
                default -> 0.0;
            };
        }else {
            return switch (strategyName){
                case "SPATIAL_CONSENSUS" -> 0.60;
                case "USER_REPUTATION" -> 0.40;
                default -> 0.0;
            };
        }
    }
}
