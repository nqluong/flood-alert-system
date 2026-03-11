package org.project.floodalert.floodprocessor.service.scoring;

import org.project.floodalert.floodprocessor.dto.request.ReportMessage;

/**
 * Strategy interface cho Scoring Engine.
 * Mỗi implementation đại diện cho một tiêu chí chấm điểm độc lập.
 *
 * <p>Áp dụng <b>Strategy Pattern</b> theo OCP: thêm tiêu chí mới
 * chỉ cần tạo thêm {@code @Component} implement interface này,
 * không cần sửa {@link ReportScoringEngine}.</p>
 */
public interface ReportScoringStrategy {

    /**
     * Tính điểm dựa trên tiêu chí riêng của từng strategy.
     *
     * @param msg báo cáo ngập cần chấm điểm
     * @return điểm số (không âm), cao hơn = đáng tin cậy hơn
     */
    double calculateScore(ReportMessage msg);

    boolean isApplicable(ReportMessage msg);

    String getStrategyName();
}
