package org.project.floodalert.floodprocessor.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình Kafka Consumer riêng cho Module 5 (Event Aggregator).
 * Consumer group này ({@code flood-event-aggregator-group}) tách biệt hoàn toàn
 * với consumer group của pipeline ingestion, đảm bảo hai pipeline không xung đột.
 *
 * Bean được đặt tên {@code aggregatorKafkaListenerContainerFactory} để
 * {@link org.project.floodalert.floodprocessor.kafka.EventAggregatorListener}
 * chỉ định qua {@code containerFactory}.
 */
@Slf4j
@Configuration
public class AggregatorKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /** Group ID riêng cho aggregator, tách biệt với pipeline ingestion */
    @Value("${app.aggregator.kafka.consumer.group-id:flood-event-aggregator-group}")
    private String aggregatorGroupId;

    /** Số luồng consumer song song của aggregator */
    @Value("${app.aggregator.kafka.consumer.concurrency:3}")
    private Integer aggregatorConcurrency;

    /** Số lượng records tối đa mỗi lần poll */
    @Value("${app.aggregator.kafka.consumer.max-poll-records:100}")
    private Integer aggregatorMaxPollRecords;


    /**
     * Consumer Factory cho Module 5 với group ID riêng biệt.
     * Sử dụng String deserializer – JSON được parse thủ công trong listener.
     */
    @Bean
    public ConsumerFactory<String, String> aggregatorConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, aggregatorGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, aggregatorMaxPollRecords);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Container Factory batch cho Module 5.
     * Cho phép listener nhận một {@code List<String>} mỗi lần poll,
     * sau đó xử lý song song trong {@link org.project.floodalert.floodprocessor.service.aggregator.FloodEventProcessorService}.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> aggregatorKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(aggregatorConsumerFactory());
        factory.setBatchListener(true);
        factory.setConcurrency(aggregatorConcurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        log.info("Khởi tạo Aggregator Kafka Listener Factory – concurrency: {}, batch-mode: true",
                aggregatorConcurrency);

        return factory;
    }
}
