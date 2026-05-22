package org.project.floodalert.floodcore.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.dto.event.ReportStatusUpdateEvent;
import org.project.floodalert.floodcore.model.UserReport;
import org.project.floodalert.floodcore.repository.UserReportRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportStatusSyncListener {

    private final UserReportRepository userReportRepository;

    /**
     * Lắng nghe event từ topic report-status-update
     * và cập nhật status của UserReport trong database
     */
    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic.report-status:report-status-update}",
            groupId = "${app.kafka.consumer.report-status.group-id:report-status-sync-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReportStatusUpdate(ReportStatusUpdateEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("[REPORT-STATUS-SYNC] Nhận event: reportId={}, status={}, eventId={}, score={}",
                    event.getReportId(), event.getStatus(), event.getEventId(), event.getScore());

            // Tìm report trong database
            Optional<UserReport> reportOpt = userReportRepository.findByReportId(event.getReportId());

            if (reportOpt.isEmpty()) {
                log.warn("[REPORT-STATUS-SYNC] Không tìm thấy report: reportId={}", event.getReportId());
                acknowledgment.acknowledge();
                return;
            }

            UserReport report = reportOpt.get();
            String oldStatus = report.getStatus();

            // Lưu floodEventId từ processor (nếu chưa có)
            if (event.getEventId() != null && report.getFloodEventId() == null) {
                report.setFloodEventId(event.getEventId());
            }

            // Cập nhật status
            report.setStatus(event.getStatus());

            // Nếu REJECTED, lưu lý do
            if ("REJECTED".equals(event.getStatus())) {
                report.setRejectReason(event.getRejectReason());
                report.setReviewedAt(LocalDateTime.now());
            }

            // Nếu APPROVED, xóa reject reason (nếu có)
            if ("APPROVED".equals(event.getStatus())) {
                report.setRejectReason(null);
                report.setReviewedAt(LocalDateTime.now());
            }

            userReportRepository.save(report);

            log.info("[REPORT-STATUS-SYNC] Cập nhật thành công: reportId={}, {} → {}, eventId={}",
                    event.getReportId(), oldStatus, event.getStatus(), event.getEventId());

            // Commit offset
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("[REPORT-STATUS-SYNC] Lỗi xử lý event: reportId={}, error={}",
                    event.getReportId(), e.getMessage(), e);
            
            // Không acknowledge -> Kafka sẽ retry
            throw e;
        }
    }
}
