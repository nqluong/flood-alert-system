package org.project.floodalert.ingestion.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.project.floodalert.ingestion.service.MessageProcessingService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorTelemetryListener implements IMqttMessageListener {

    private final MessageProcessingService messageProcessingService;

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        try {
            String payload = new String(mqttMessage.getPayload());
            messageProcessingService.process(topic, payload);
        } catch (Exception e) {
            log.error("Error in MQTT message listener - topic: {}, error: {}",
                    topic, e.getMessage(), e);
        }

    }
}
