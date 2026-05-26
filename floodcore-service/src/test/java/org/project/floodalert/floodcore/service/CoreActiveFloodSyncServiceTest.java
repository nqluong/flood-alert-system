package org.project.floodalert.floodcore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodcore.enums.LifecycleEventType;
import org.project.floodalert.floodcore.model.CoreActiveFlood;
import org.project.floodalert.floodcore.repository.CoreActiveFloodRepository;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoreActiveFloodSyncServiceTest {

    @Mock
    private CoreActiveFloodRepository coreActiveFloodRepository;

    @Mock
    private FloodGeoCache floodGeoCache;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private CoreActiveFloodSyncService service;

    private FloodLifecycleEvent event;

    @BeforeEach
    void setUp() {
        event = new FloodLifecycleEvent();
        event.setEventId("flood-001");
        event.setLat(21.01);
        event.setLon(105.85);
        event.setLocation("Ha Noi");
        event.setWaterLevel(0.8);
        event.setSeverityLevel("MEDIUM");
        event.setSource("USER_REPORT");
    }

    @Test
    void onFloodLifecycleEvent_eventNull_acknowledges() {
        service.onFloodLifecycleEvent(null, acknowledgment);

        verify(acknowledgment).acknowledge();
        verifyNoInteractions(coreActiveFloodRepository, floodGeoCache);
    }

    @Test
    void onFloodLifecycleEvent_typeNull_acknowledgesOnly() {
        event.setType(null);

        service.onFloodLifecycleEvent(event, acknowledgment);

        verify(acknowledgment).acknowledge();
        verifyNoInteractions(coreActiveFloodRepository, floodGeoCache);
    }

    @Test
    void onFloodLifecycleEvent_resolved_existingRecord_deletesDbAndCache() {
        event.setType(LifecycleEventType.RESOLVED);

        when(coreActiveFloodRepository.existsById("flood-001")).thenReturn(true);

        service.onFloodLifecycleEvent(event, acknowledgment);

        verify(coreActiveFloodRepository).existsById("flood-001");
        verify(coreActiveFloodRepository).deleteById("flood-001");
        verify(floodGeoCache).removeFloodCache("flood-001");
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onFloodLifecycleEvent_resolved_recordNotFound_removesCacheOnly() {
        event.setType(LifecycleEventType.RESOLVED);

        when(coreActiveFloodRepository.existsById("flood-001")).thenReturn(false);

        service.onFloodLifecycleEvent(event, acknowledgment);

        verify(coreActiveFloodRepository).existsById("flood-001");
        verify(coreActiveFloodRepository, never()).deleteById(any());
        verify(floodGeoCache).removeFloodCache("flood-001");
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onFloodLifecycleEvent_created_newRecord_savesAndCaches() {
        event.setType(LifecycleEventType.CREATED);

        when(coreActiveFloodRepository.findById("flood-001"))
                .thenReturn(Optional.empty());

        service.onFloodLifecycleEvent(event, acknowledgment);

        verify(coreActiveFloodRepository).findById("flood-001");
        verify(coreActiveFloodRepository).save(any(CoreActiveFlood.class));
        verify(floodGeoCache).cacheActiveFlood(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onFloodLifecycleEvent_updated_existingRecord_savesAndCaches() {
        event.setType(LifecycleEventType.UPDATED);

        CoreActiveFlood existingFlood = CoreActiveFlood.builder()
                .eventId("flood-001")
                .source("OLD_SOURCE")
                .build();

        when(coreActiveFloodRepository.findById("flood-001"))
                .thenReturn(Optional.of(existingFlood));

        service.onFloodLifecycleEvent(event, acknowledgment);

        verify(coreActiveFloodRepository).findById("flood-001");
        verify(coreActiveFloodRepository).save(existingFlood);
        verify(floodGeoCache).cacheActiveFlood(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onFloodLifecycleEvent_upsert_withNullValues_stillSavesAndCaches() {
        event.setType(LifecycleEventType.CREATED);
        event.setLat(null);
        event.setLon(null);
        event.setWaterLevel(null);
        event.setSource(null);

        when(coreActiveFloodRepository.findById("flood-001"))
                .thenReturn(Optional.empty());

        service.onFloodLifecycleEvent(event, acknowledgment);

        verify(coreActiveFloodRepository).findById("flood-001");
        verify(coreActiveFloodRepository).save(any(CoreActiveFlood.class));
        verify(floodGeoCache).cacheActiveFlood(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onFloodLifecycleEvent_repositoryThrowsException_stillAcknowledges() {
        event.setType(LifecycleEventType.CREATED);

        when(coreActiveFloodRepository.findById("flood-001"))
                .thenThrow(new RuntimeException("DB error"));

        service.onFloodLifecycleEvent(event, acknowledgment);

        verify(coreActiveFloodRepository).findById("flood-001");
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(floodGeoCache);
    }
}
