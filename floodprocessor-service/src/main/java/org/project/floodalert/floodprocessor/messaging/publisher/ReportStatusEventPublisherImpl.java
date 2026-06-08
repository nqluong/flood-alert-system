package org.project.floodalert.floodprocessor.messaging.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.event.ReportStatusUpdateEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportStatusEventPublisherImpl implements ReportStatusEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.report-status:report-status-update}")
    private String reportStatusTopic;

    @Override
    public void publish(ReportStatusUpdateEvent event) {
        if (event == null || event.getReportId() == null) {
            log.warn("Report status event hoặc reportId bị null, bỏ qua publish");
            return;
        }

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(reportStatusTopic, event.getReportId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Gửi Report Status Event thất bại: reportId={}, status={}: {}",
                        event.getReportId(), event.getStatus(), ex.getMessage());
            } else {
                var meta = result.getRecordMetadata();
                log.info("Report Status Event đã gửi thành công: reportId={}, status={}, eventId={}, " +
                                "topic={}, partition={}, offset={}",
                        event.getReportId(), event.getStatus(), event.getEventId(),
                        meta.topic(), meta.partition(), meta.offset());
            }
        });

        log.debug("Đã đặt lịch gửi Report Status Event [{}] status [{}] lên topic [{}]",
                event.getReportId(), event.getStatus(), reportStatusTopic);
    }
}
