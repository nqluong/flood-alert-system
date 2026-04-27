package org.project.floodalert.notification.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;


@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return props;
    }

    private ConsumerFactory<String, Object> createConsumerFactory(String valueDefaultType) {
        Map<String, Object> props = baseProps();
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueDefaultType);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    private ConcurrentKafkaListenerContainerFactory<String, Object> createContainerFactory(
            ConsumerFactory<String, Object> consumerFactory, 
            int concurrency) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(concurrency);
        return factory;
    }


    @Bean
    public ConsumerFactory<String, Object> telemetryConsumerFactory() {
        return createConsumerFactory("org.project.floodalert.notification.dto.response.ProcessedSensorData");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> notificationKafkaListenerContainerFactory() {
        return createContainerFactory(telemetryConsumerFactory(), 1);
    }


    @Bean
    public ConsumerFactory<String, Object> lifecycleConsumerFactory() {
        return createConsumerFactory("org.project.floodalert.notification.dto.event.FloodLifecycleEvent");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> lifecycleKafkaListenerContainerFactory() {
        return createContainerFactory(lifecycleConsumerFactory(), 1);
    }

    @Bean
    public ConsumerFactory<String, Object> fcmTokenConsumerFactory() {
        return createConsumerFactory("org.project.floodalert.notification.dto.event.FcmTokenEvent");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> fcmTokenKafkaListenerContainerFactory() {
        return createContainerFactory(fcmTokenConsumerFactory(), 2);
    }
}
