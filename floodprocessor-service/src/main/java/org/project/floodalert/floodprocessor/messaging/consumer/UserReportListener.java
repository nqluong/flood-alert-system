package org.project.floodalert.floodprocessor.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.UserReportEvent;
import org.project.floodalert.floodprocessor.service.processing.ReportProcessingUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserReportListener {

    private final ReportProcessingUseCase reportProcessingUseCase;

    /**
     * Nhận một {@link UserReportEvent} từ topic {@code user-report-events}.
     *
     * @param event          payload báo cáo được deserialize tự động
     * @param acknowledgment dùng để commit offset thủ công sau khi xử lý
     */
    @KafkaListener(
            topics = "${app.kafka.topic.user-report}",
            groupId = "${app.kafka.user-report.consumer.group-id:user-report-processor-group}",
            containerFactory = "userReportKafkaListenerContainerFactory"
    )
    public void consume(UserReportEvent event, Acknowledgment acknowledgment) {
        log.info("[USER-REPORT-LISTENER] Nhận báo cáo mới: reportId={}, userId={}, lat={}, lon={}",
                event.getReportId(), event.getUserId(), event.getLat(), event.getLon());

        try {
            reportProcessingUseCase.process(event);
            acknowledgment.acknowledge();

            log.info("[USER-REPORT-LISTENER] Xử lý thành công — reportId={}", event.getReportId());

        } catch (Exception e) {
            log.error("[USER-REPORT-LISTENER] Lỗi khi xử lý reportId={}: {}",
                    event.getReportId(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}
