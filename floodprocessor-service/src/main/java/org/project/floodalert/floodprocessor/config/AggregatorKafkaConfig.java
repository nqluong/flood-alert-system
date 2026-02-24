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
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;


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
    public ConsumerFactory<String, Object> aggregatorConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, aggregatorGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, aggregatorMaxPollRecords);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> aggregatorKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(aggregatorConsumerFactory());
        factory.setBatchListener(true);
        factory.setConcurrency(aggregatorConcurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}
