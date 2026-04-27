package org.project.floodalert.floodprocessor.service.scoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.response.GeminiVisionResponse;
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

        try {
            GeminiVisionResponse response = geminiApiClient.analyzeFloodImage(msg);

            // Validate response
            if (response == null) {
                log.error("[SCORING][AI-VISION] Gemini trả về null response cho reportId={}", msg.getReportId());
                return 0.0;
            }

            // Chuẩn hoá điểm từ thang AI (0-100) sang thang hệ thống (0.0-1.0)
            double normalizedScore = response.getConfidenceScore() / 100.0;

            log.info("[SCORING][AI-VISION] reportId={} → isFlooded={}, waterLevel={}, confidenceScore={}, normalizedScore={}, reasoning='{}'",
                    msg.getReportId(),
                    response.isFlooded(),
                    response.getWaterLevelEstimate(),
                    response.getConfidenceScore(),
                    normalizedScore,
                    response.getReasoning());

            // Nếu AI xác định KHÔNG phải ngập nhưng confidence thấp, log warning
            if (!response.isFlooded() && response.getConfidenceScore() < 30) {
                log.warn("[SCORING][AI-VISION] reportId={} - AI không chắc chắn (confidence < 30): {}", 
                        msg.getReportId(), response.getReasoning());
            }

            return normalizedScore;

        } catch (IllegalArgumentException e) {
            log.error("[SCORING][AI-VISION] Invalid input cho reportId={}: {}", 
                    msg.getReportId(), e.getMessage());
            return 0.0;
            
        } catch (RuntimeException e) {
            // Các lỗi từ GeminiWebClientImpl (network, timeout, parse error)
            log.error("[SCORING][AI-VISION] Lỗi khi gọi Gemini API cho reportId={}: {}", 
                    msg.getReportId(), e.getMessage());
            
            // Log stack trace nếu là lỗi không mong đợi
            if (!(e.getMessage().contains("timeout") || e.getMessage().contains("download"))) {
                log.error("[SCORING][AI-VISION] Stack trace:", e);
            }
            
            return 0.0;
            
        } catch (Exception e) {
            // Fallback an toàn: không để lỗi API làm gián đoạn pipeline xử lý báo cáo
            log.error("[SCORING][AI-VISION] Lỗi không xác định cho reportId={}: {}", 
                    msg.getReportId(), e.getMessage(), e);
            return 0.0;
        }
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
