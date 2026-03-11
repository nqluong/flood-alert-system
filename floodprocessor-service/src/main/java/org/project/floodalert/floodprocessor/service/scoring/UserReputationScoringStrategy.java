package org.project.floodalert.floodprocessor.service.scoring;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.springframework.stereotype.Component;

/**
 * Strategy chấm điểm dựa trên điểm uy tín của người dùng (User Reputation).
 *
 * <p><b>TODO:</b> Lấy {@code trustScore} từ bảng {@code trust_scores} hoặc cache Redis
 * theo {@link ReportMessage#getUserId()} để phản ánh lịch sử báo cáo chính xác của user.</p>
 *
 * <p>Trọng số tối đa: <b>10 điểm</b>.</p>
 */
@Slf4j
@Component
public class UserReputationScoringStrategy implements ReportScoringStrategy {

    @Override
    public double calculateScore(ReportMessage msg) {
        log.debug("[SCORING][REPUTATION] Đang tra cứu uy tín cho userId={}, reportId={}",
                msg.getUserId(), msg.getReportId());

        // TODO: Tra cứu trust score của userId từ DB/Redis
        double score = 10.0;

        log.debug("[SCORING][REPUTATION] userId={} → score={}", msg.getUserId(), score);
        return score;
    }

    @Override
    public boolean isApplicable(ReportMessage msg) {
        return true;
    }

    @Override
    public String getStrategyName() {
        return "USER_REPUTATION";
    }
}
