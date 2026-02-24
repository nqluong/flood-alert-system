package org.project.floodalert.floodprocessor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDispatcher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.output}")
    private String outputTopic;

    public void send(ProcessedSensorData data) {
        String sensorId = data.getSensorId();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(outputTopic, sensorId, data);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                handleSendFailure(sensorId, ex);
            } else {
                handleSendSuccess(sensorId, result);
            }
        });
    }

    private void handleSendSuccess(String sensorId, SendResult<String, Object> result) {
        var metadata = result.getRecordMetadata();
        log.debug("Kafka sent: sensor={}, topic={}, partition={}, offset={}",
                sensorId,
                metadata.topic(),
                metadata.partition(),
                metadata.offset());
    }

    private void handleSendFailure(String sensorId, Throwable exception) {
        log.error("Kafka send failed cho sensor: {} - Dữ liệu đã lưu DB nhưng chưa gửi Kafka",
                sensorId, exception);
    }
}
