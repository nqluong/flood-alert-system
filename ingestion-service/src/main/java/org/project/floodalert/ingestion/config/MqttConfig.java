package org.project.floodalert.ingestion.config;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.project.floodalert.ingestion.mqtt.SensorTelemetryListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MqttConfig {
    private final SensorTelemetryListener sensorTelemetryListener;

    @Value("${mqtt.borker-url}")
    private String borkerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.topic-pattern}")
    private String topicPattern;

    @Value("${mqtt.qos:1}")
    private int qos;

    @Value("${mqtt.auto-reconnect:true}")
    private boolean autoReconnect;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    private MqttClient mqttClient;

    @Bean
    public MqttClient mqttClient() throws MqttException {
        mqttClient = new MqttClient(borkerUrl, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(autoReconnect);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);

        if (!username.isBlank()) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }

        mqttClient.connect(options);

        mqttClient.subscribe(topicPattern, qos, sensorTelemetryListener);
        log.info("Subscribed to MQTT topic: {} with QoS: {}", topicPattern, qos);


        return mqttClient;
    }

    @PreDestroy
    public void cleanup() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                log.info("MQTT client disconnected");
            } catch (MqttException e) {
                log.error("Error disconnecting MQTT client", e);
            }
        }
    }
}
