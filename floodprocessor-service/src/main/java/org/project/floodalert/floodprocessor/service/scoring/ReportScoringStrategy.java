package org.project.floodalert.floodprocessor.service.scoring;

import org.project.floodalert.floodprocessor.dto.request.ReportMessage;


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
