package org.project.floodalert.floodprocessor.service.processing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.project.floodalert.floodprocessor.dto.event.ReportStatusUpdateEvent;
import org.project.floodalert.floodprocessor.dto.request.UserReportEvent;
import org.project.floodalert.floodprocessor.dto.response.ScoringResult;
import org.project.floodalert.floodprocessor.messaging.publisher.ReportStatusEventPublisher;
import org.project.floodalert.floodprocessor.messaging.publisher.ReputationEventPublisher;
import org.project.floodalert.floodprocessor.model.EventContributor;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.service.cache.impl.SharedRedisGeoService;
import org.project.floodalert.floodprocessor.service.persistence.FloodEventPersistenceService;
import org.project.floodalert.floodprocessor.service.scoring.impl.ReportScoringEngine;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportProcessorServiceImplTest {

        @Mock
        private SharedRedisGeoService redisGeoService;

        @Mock
        private ReportScoringEngine scoringEngine;

        @Mock
        private FloodEventPersistenceService persistenceService;

        @Mock
        private StringRedisTemplate stringRedisTemplate;

        @Mock
        private KafkaTemplate<String, Object> kafkaTemplate;

        @Mock
        private ReputationEventPublisher reputationEventPublisher;

        @Mock
        private ReportStatusEventPublisher reportStatusEventPublisher;

        @Mock
        private ValueOperations<String, String> valueOperations;

        @InjectMocks
        private ReportProcessorServiceImpl service;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(service, "lifecycleTopic", "flood-lifecycle");
                when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        private UserReportEvent buildEvent() {
                return UserReportEvent.builder()
                                .reportId("report-001")
                                .userId(UUID.randomUUID())
                                .lat(10.5)
                                .lon(106.5)
                                .build();
        }

        private ScoringResult buildScore(double total) {
                return ScoringResult.builder()
                                .totalScore(total)
                                .aiScore(total)
                                .spatialScore(total)
                                .reputationScore(total)
                                .build();
        }

        private FloodEvent buildFloodEvent(String status) {
                return FloodEvent.builder()
                                .id(UUID.randomUUID())
                                .eventId("evt-001")
                                .status(status)
                                .lat(10.5)
                                .lon(106.5)
                                .voteCount(1)
                                .build();
        }

        @Test
        void process_spam_rejectsImmediately() {
                when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(false);

                service.process(buildEvent());

                ArgumentCaptor<ReportStatusUpdateEvent> captor = ArgumentCaptor.forClass(ReportStatusUpdateEvent.class);
                verify(reportStatusEventPublisher).publish(captor.capture());
                assertEquals("REJECTED", captor.getValue().getStatus());
                assertEquals("report-001", captor.getValue().getReportId());
        }

        @Test
        void process_noNearbyEvent_newReportRejected_penalizesUser() {
                when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
                when(redisGeoService.findNearbyActiveFlood(anyDouble(), anyDouble(), anyDouble()))
                                .thenReturn(Optional.empty());
                when(scoringEngine.evaluateScore(any())).thenReturn(buildScore(0.1));
                when(persistenceService.handleNewReport(any(), anyDouble())).thenReturn(buildFloodEvent("REJECTED"));

                service.process(buildEvent());
        }

        @Test
        void process_noNearbyEvent_newReportPending_holdsReward() {
                when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
                when(redisGeoService.findNearbyActiveFlood(anyDouble(), anyDouble(), anyDouble()))
                                .thenReturn(Optional.empty());
                when(scoringEngine.evaluateScore(any())).thenReturn(buildScore(0.5));
                when(persistenceService.handleNewReport(any(), anyDouble())).thenReturn(buildFloodEvent("PENDING"));

                service.process(buildEvent());
        }

        @Test
        void process_noNearbyEvent_newReportActive_rewardsPioneer() {
                when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
                when(redisGeoService.findNearbyActiveFlood(anyDouble(), anyDouble(), anyDouble()))
                                .thenReturn(Optional.empty());
                when(scoringEngine.evaluateScore(any())).thenReturn(buildScore(0.9));
                when(persistenceService.handleNewReport(any(), anyDouble())).thenReturn(buildFloodEvent("ACTIVE"));

                service.process(buildEvent());

                ArgumentCaptor<ReportStatusUpdateEvent> captor = ArgumentCaptor.forClass(ReportStatusUpdateEvent.class);
                verify(reportStatusEventPublisher).publish(captor.capture());
                assertEquals("APPROVED", captor.getValue().getStatus());
                assertEquals("evt-001", captor.getValue().getEventId());
        }

        @Test
        void process_nearbyActiveEvent_fastUpdatePath() {
                when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
                when(redisGeoService.findNearbyActiveFlood(anyDouble(), anyDouble(), anyDouble()))
                                .thenReturn(Optional.of("evt-001"));
                when(persistenceService.getEventStatus("evt-001")).thenReturn("ACTIVE");
                when(persistenceService.handleFastUpdate(any(), eq("evt-001"))).thenReturn(buildFloodEvent("ACTIVE"));

                service.process(buildEvent());
        }

        @Test
        void process_nearbyPendingEvent_stillPending_publishesPendingStatus() {
                when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
                when(redisGeoService.findNearbyActiveFlood(anyDouble(), anyDouble(), anyDouble()))
                                .thenReturn(Optional.of("evt-001"));
                when(persistenceService.getEventStatus("evt-001")).thenReturn("PENDING");
                when(scoringEngine.evaluateScore(any())).thenReturn(buildScore(0.5));
                when(persistenceService.handleClusterUpdate(any(), eq("evt-001"), anyDouble()))
                                .thenReturn(buildFloodEvent("PENDING"));

                service.process(buildEvent());
        }

        @Test
        void process_nearbyPendingEvent_transitionsToActive_rewardsAllContributors() {
                UUID eventUuid = UUID.randomUUID();
                FloodEvent activeEvent = FloodEvent.builder()
                                .id(eventUuid)
                                .eventId("evt-001")
                                .status("ACTIVE")
                                .lat(10.5)
                                .lon(106.5)
                                .voteCount(2)
                                .build();

                List<EventContributor> contributors = List.of(
                                EventContributor.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                                                .role("PIONEER").build(),
                                EventContributor.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                                                .role("VERIFIER").build(),
                                EventContributor.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                                                .role("UNKNOWN").build());

                when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
                when(redisGeoService.findNearbyActiveFlood(anyDouble(), anyDouble(), anyDouble()))
                                .thenReturn(Optional.of("evt-001"));
                when(persistenceService.getEventStatus("evt-001")).thenReturn("PENDING");
                when(scoringEngine.evaluateScore(any())).thenReturn(buildScore(0.9));
                when(persistenceService.handleClusterUpdate(any(), eq("evt-001"), anyDouble()))
                                .thenReturn(activeEvent);
                when(persistenceService.getContributorsByEventId(eventUuid)).thenReturn(contributors);

                service.process(buildEvent());
        }
}
