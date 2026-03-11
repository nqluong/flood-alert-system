package org.project.floodalert.floodcore.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.dto.event.UserReportEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserReportEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.user-report:user-report-events}")
    private String userReportTopic;

    public void publish(UserReportEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(userReportTopic, event.getReportId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[UserReportProducer] Gửi event thất bại - reportId={}, lỗi: {}",
                        event.getReportId(), ex.getMessage(), ex);
            } else {
                var meta = result.getRecordMetadata();
                log.info("[UserReportProducer] Gửi event thành công - reportId={}, topic={}, partition={}, offset={}",
                        event.getReportId(), meta.topic(), meta.partition(), meta.offset());
            }
        });
    }
}

