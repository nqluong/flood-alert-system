package org.project.floodalert.ingestion.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.project.floodalert.ingestion.service.MessageProcessingService;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorTelemetryListener implements IMqttMessageListener {

    private final MessageProcessingService messageProcessingService;

    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        virtualThreadExecutor.execute(() -> {
            try {
                messageProcessingService.process(topic, payload);
            } catch (Exception e) {
                log.error("Error processing MQTT message - topic: {}, error: {}", topic, e.getMessage(), e);
            }
        });
    }
}
