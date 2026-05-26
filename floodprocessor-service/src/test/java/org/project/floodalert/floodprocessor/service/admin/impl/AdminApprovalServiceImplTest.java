package org.project.floodalert.floodprocessor.service.admin.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.enums.ContributorRole;
import org.project.floodalert.floodprocessor.messaging.publisher.LifecycleEventPublisher;
import org.project.floodalert.floodprocessor.messaging.publisher.ReputationEventPublisher;
import org.project.floodalert.floodprocessor.model.EventContributor;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.project.floodalert.floodprocessor.service.persistence.FloodEventPersistenceService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminApprovalServiceImplTest {

    @Mock
    private FloodEventRepository floodEventRepository;

    @Mock
    private FloodEventPersistenceService persistenceService;

    @Mock
    private ReputationEventPublisher reputationPublisher;

    @Mock
    private LifecycleEventPublisher lifecyclePublisher;

    @InjectMocks
    private AdminApprovalServiceImpl service;

    // ==================== approveEvent ====================

    @Test
    void approveEvent_noContributors_publishesLifecycleOnly() {
        FloodEvent event = buildFloodEvent();
        when(floodEventRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));
        when(floodEventRepository.save(any())).thenReturn(event);
        when(persistenceService.getContributorsByEventId(any())).thenReturn(List.of());

        service.approveEvent("evt-1");

        verify(lifecyclePublisher).publish(any());
        verify(reputationPublisher, never()).publish(any());
    }

    @Test
    void approveEvent_withPioneerContributor_publishesReputationAndLifecycle() {
        FloodEvent event = buildFloodEvent();
        EventContributor pioneer = buildContributor(ContributorRole.PIONEER.name());

        when(floodEventRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));
        when(floodEventRepository.save(any())).thenReturn(event);
        when(persistenceService.getContributorsByEventId(any())).thenReturn(List.of(pioneer));

        service.approveEvent("evt-1");

        verify(reputationPublisher, times(1)).publish(any());
        verify(lifecyclePublisher).publish(any());
    }

    @Test
    void approveEvent_withVerifierContributor_publishesReputationAndLifecycle() {
        FloodEvent event = buildFloodEvent();
        EventContributor verifier = buildContributor(ContributorRole.VERIFIER.name());

        when(floodEventRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));
        when(floodEventRepository.save(any())).thenReturn(event);
        when(persistenceService.getContributorsByEventId(any())).thenReturn(List.of(verifier));

        service.approveEvent("evt-1");

        verify(reputationPublisher, times(1)).publish(any());
    }

    @Test
    void approveEvent_withUnknownRoleContributor_defaultsToVerifierPoints() {
        FloodEvent event = buildFloodEvent();
        EventContributor unknown = buildContributor("UNKNOWN_ROLE");

        when(floodEventRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));
        when(floodEventRepository.save(any())).thenReturn(event);
        when(persistenceService.getContributorsByEventId(any())).thenReturn(List.of(unknown));

        service.approveEvent("evt-1");

        verify(reputationPublisher, times(1)).publish(any());
    }

    @Test
    void approveEvent_withNullWaterLevel_handlesNullSafely() {
        FloodEvent event = buildFloodEvent();
        event.setWaterLevel(null);

        when(floodEventRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));
        when(floodEventRepository.save(any())).thenReturn(event);
        when(persistenceService.getContributorsByEventId(any())).thenReturn(List.of());

        service.approveEvent("evt-1");

        verify(lifecyclePublisher).publish(any());
    }

    @Test
    void approveEvent_eventNotFound_throwsAppException() {
        when(floodEventRepository.findByEventId("missing")).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> service.approveEvent("missing"));
    }


    @Test
    void rejectEvent_noContributors_onlySavesEvent() {
        FloodEvent event = buildFloodEvent();
        when(floodEventRepository.findByEventId("evt-2")).thenReturn(Optional.of(event));
        when(floodEventRepository.save(any())).thenReturn(event);
        when(persistenceService.getContributorsByEventId(any())).thenReturn(List.of());

        service.rejectEvent("evt-2");

        verify(reputationPublisher, never()).publish(any());
    }

    @Test
    void rejectEvent_withContributors_publishesPenaltyForEach() {
        FloodEvent event = buildFloodEvent();
        EventContributor c1 = buildContributor(ContributorRole.PIONEER.name());
        EventContributor c2 = buildContributor(ContributorRole.VERIFIER.name());

        when(floodEventRepository.findByEventId("evt-2")).thenReturn(Optional.of(event));
        when(floodEventRepository.save(any())).thenReturn(event);
        when(persistenceService.getContributorsByEventId(any())).thenReturn(List.of(c1, c2));

        service.rejectEvent("evt-2");

        verify(reputationPublisher, times(2)).publish(any());
    }

    @Test
    void rejectEvent_eventNotFound_throwsAppException() {
        when(floodEventRepository.findByEventId("missing")).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> service.rejectEvent("missing"));
    }


    private FloodEvent buildFloodEvent() {
        FloodEvent event = new FloodEvent();
        event.setId(UUID.randomUUID());
        event.setStatus("PENDING");
        event.setConfidenceScore(BigDecimal.valueOf(0.5));
        event.setWaterLevel(BigDecimal.valueOf(1.2));
        return event;
    }

    private EventContributor buildContributor(String role) {
        EventContributor contributor = new EventContributor();
        contributor.setUserId(UUID.randomUUID());
        contributor.setRole(role);
        return contributor;
    }
}