package org.project.floodalert.floodprocessor.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.project.floodalert.floodprocessor.dto.request.UserReportEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình Kafka Consumer riêng biệt cho luồng xử lý báo cáo người dùng.
 *
 * <p>Tách biệt với {@link KafkaConfig} (pipeline IoT sensor) để đảm bảo:
 * <ul>
 *   <li>Group ID độc lập, không ảnh hưởng đến offset của pipeline IoT.</li>
 *   <li>Type mapping riêng cho {@link UserReportEvent}.</li>
 *   <li>Chế độ single-message (không batch) phù hợp với báo cáo thời gian thực.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
public class UserReportKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.user-report.consumer.group-id:user-report-processor-group}")
    private String groupId;

    @Value("${app.kafka.user-report.consumer.concurrency:2}")
    private Integer concurrency;

    @Bean
    public ConsumerFactory<String, UserReportEvent> userReportConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "org.project.floodalert.floodprocessor.dto.request.UserReportEvent");

        log.info("[USER-REPORT-KAFKA] Khởi tạo UserReport Consumer Factory — bootstrap={}, group-id={}",
                bootstrapServers, groupId);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserReportEvent>
    userReportKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, UserReportEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(userReportConsumerFactory());
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        log.info("[USER-REPORT-KAFKA] Khởi tạo UserReport Listener Container Factory — concurrency={}",
                concurrency);
        return factory;
    }
}
