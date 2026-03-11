package org.project.floodalert.floodprocessor.service.scoring;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.springframework.stereotype.Component;

/**
 * Strategy chấm điểm dựa trên phân tích hình ảnh bằng AI Vision.
 *
 * <p><b>TODO:</b> Tích hợp gọi AI Vision API (Google Vision / AWS Rekognition)
 * để phát hiện dấu hiệu ngập thực sự trong ảnh {@link ReportMessage#getImageUrl()}.</p>
 *
 * <p>Trọng số tối đa: <b>50 điểm</b> — tiêu chí quan trọng nhất.</p>
 */
@Slf4j
@Component
public class AiVisionScoringStrategy implements ReportScoringStrategy {

    @Override
    public double calculateScore(ReportMessage msg) {
        log.debug("[SCORING][AI-VISION] Đang phân tích ảnh cho reportId={}, imageUrl={}",
                msg.getReportId(), msg.getImageUrl());

        // TODO: Gọi AI Vision API để phân tích ảnh và trả về điểm thực
        double score = 50.0;

        log.debug("[SCORING][AI-VISION] reportId={} → score={}", msg.getReportId(), score);
        return score;
    }

    @Override
    public boolean isApplicable(ReportMessage msg) {
        return msg.getImageUrl() != null && !msg.getImageUrl().isEmpty();
    }

    @Override
    public String getStrategyName() {
        return "AI_VISION";
    }
}
