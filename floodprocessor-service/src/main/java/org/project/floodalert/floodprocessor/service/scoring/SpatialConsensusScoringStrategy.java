package org.project.floodalert.floodprocessor.service.scoring;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.springframework.stereotype.Component;

/**
 * Strategy chấm điểm dựa trên sự đồng thuận không gian (Spatial Consensus).
 *
 * <p><b>TODO:</b> Truy vấn số lượng báo cáo khác trong vòng bán kính gần đó
 * (ví dụ: trong 500m, trong 1 giờ qua) từ DB hoặc Redis để đánh giá mức độ
 * đồng thuận từ cộng đồng.</p>
 *
 * <p>Trọng số tối đa: <b>20 điểm</b>.</p>
 */
@Slf4j
@Component
public class SpatialConsensusScoringStrategy implements ReportScoringStrategy {

    @Override
    public double calculateScore(ReportMessage msg) {
        log.debug("[SCORING][SPATIAL] Đang kiểm tra đồng thuận không gian cho reportId={}, lat={}, lon={}",
                msg.getReportId(), msg.getLat(), msg.getLon());

        // TODO: Truy vấn số báo cáo lân cận trong DB/Redis và tính tỷ lệ đồng thuận
        double score = 20.0;

        log.debug("[SCORING][SPATIAL] reportId={} → score={}", msg.getReportId(), score);
        return score;
    }

    @Override
    public boolean isApplicable(ReportMessage msg) {
        return true;
    }

    @Override
    public String getStrategyName() {
        return "SPATIAL_CONSENSUS";
    }
}
