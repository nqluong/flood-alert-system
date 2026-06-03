package org.project.floodalert.ingestion.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.project.floodalert.ingestion.service.MessageProcessingService;
import org.project.floodalert.ingestion.service.SensorIdExtractor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorTelemetryListener implements IMqttMessageListener {

    private final MessageProcessingService messageProcessingService;
    private final SensorIdExtractor sensorIdExtractor;

    private final ConcurrentHashMap<String, Executor> sensorExecutors = new ConcurrentHashMap<>();

    private final Executor fallbackExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        String sensorId = sensorIdExtractor.extract(topic);

        Executor executor = (sensorId != null && !sensorId.isEmpty())
                ? sensorExecutors.computeIfAbsent(sensorId, k -> buildSensorExecutor())
                : fallbackExecutor;

        executor.execute(() -> {
            try {
                messageProcessingService.process(topic, payload);
            } catch (Exception e) {
                log.error("Error processing MQTT message - topic: {}, error: {}", topic, e.getMessage(), e);
            }
        });
    }

    private Executor buildSensorExecutor() {
        return Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    }
}
