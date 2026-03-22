package org.project.floodalert.floodprocessor.service.scoring;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.service.UserProfileCacheService;
import org.springframework.stereotype.Component;

/**
 * Strategy chấm điểm dựa trên điểm uy tín của người dùng (User Reputation).
 *
 * <p>Lấy {@code reputation score} từ cache Redis qua {@link UserProfileCacheService}
 * theo {@link ReportMessage#getUserId()} để phản ánh lịch sử báo cáo chính xác của user.</p>
 *
 * <p><b>Tính điểm:</b> reputation_score / 100 * 10, tối đa <b>10 điểm</b>.</p>
 */
@Slf4j
@Component
public class UserReputationScoringStrategy implements ReportScoringStrategy {

    private final UserProfileCacheService userProfileCacheService;

    public UserReputationScoringStrategy(UserProfileCacheService userProfileCacheService) {
        this.userProfileCacheService = userProfileCacheService;
    }

    @Override
    public double calculateScore(ReportMessage msg) {
        log.debug("[SCORING][REPUTATION] Đang tra cứu uy tín cho userId={}, reportId={}",
                msg.getUserId(), msg.getReportId());

        // Lấy điểm uy tín từ cache (fallback 3 lớp: Redis → Auth-Service → Default=50)
        int reputationScore = userProfileCacheService.getReputationScore(msg.getUserId());

        double score = reputationScore / 100.0;

        log.debug("[SCORING][REPUTATION] userId={}, reputationScore={} → scorePoints={}",
                msg.getUserId(), reputationScore, score);
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
