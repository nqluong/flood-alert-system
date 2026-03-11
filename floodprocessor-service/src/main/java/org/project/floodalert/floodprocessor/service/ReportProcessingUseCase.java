package org.project.floodalert.floodprocessor.service;

import org.project.floodalert.floodprocessor.dto.request.UserReportEvent;

public interface ReportProcessingUseCase {

    /**
     * Xử lý một báo cáo ngập từ người dùng.
     *
     * <p>Luồng xử lý bao gồm:
     * <ol>
     *   <li>Kiểm tra báo cáo có nằm gần điểm ngập đang hoạt động (Redis Geo) không.</li>
     *   <li>Nếu CÓ: Bắn lifecycle event {@code UPDATED} và dừng (Early Exit).</li>
     *   <li>Nếu KHÔNG: Đưa báo cáo qua Scoring Engine để tính điểm độ tin cậy.</li>
     * </ol>
     * </p>
     *
     * @param event payload báo cáo nhận từ Kafka topic {@code user-report-events}
     */
    void process(UserReportEvent event);
}
