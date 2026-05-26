package org.project.floodalert.floodprocessor.messaging.publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.event.ReputationUpdateEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReputationEventPublisherImplTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ReputationEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "reputationTopic", "reputation-update-events");
    }

    @Test
    void publish_nullEvent_skips() {
        publisher.publish(null);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_nullUserId_skips() {
        ReputationUpdateEvent event = ReputationUpdateEvent.builder().userId(null).build();
        publisher.publish(event);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_validEvent_sendsToKafka() {
        ReputationUpdateEvent event = ReputationUpdateEvent.builder()
                .userId(UUID.randomUUID())
                .points(5)
                .build();

        publisher.publish(event);

        verify(kafkaTemplate).send(anyString(), anyString(), eq(event));
    }
}