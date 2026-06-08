package org.project.floodalert.floodprocessor.service.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.model.EventContributor;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.model.TrustScore;
import org.project.floodalert.floodprocessor.repository.EventContributorRepository;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.project.floodalert.floodprocessor.repository.TrustScoreRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FloodEventPersistenceServiceTest {

    @Mock
    private FloodEventRepository floodEventRepository;

    @Mock
    private TrustScoreRepository trustScoreRepository;

    @Mock
    private EventContributorRepository eventContributorRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FloodEventPersistenceService service;

    private ReportMessage buildMsg() {
        return buildMsg(null);
    }

    private ReportMessage buildMsg(String imageUrl) {
        return ReportMessage.builder()
                .reportId("report-001")
                .userId(UUID.randomUUID())
                .lat(10.5)
                .lon(106.5)
                .severityLevel("MODERATE")
                .imageUrl(imageUrl)
                .description("Flood description")
                .build();
    }

    private FloodEvent buildSavedEvent(String status) {
        return FloodEvent.builder()
                .id(UUID.randomUUID())
                .eventId("evt-001")
                .status(status)
                .lat(10.5)
                .lon(106.5)
                .voteCount(1)
                .build();
    }

    private FloodEvent buildSavedEventWithRawData(String status) {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("existing", "data");
        return FloodEvent.builder()
                .id(UUID.randomUUID())
                .eventId("evt-001")
                .status(status)
                .lat(10.5)
                .lon(106.5)
                .voteCount(1)
                .rawData(rawData)
                .build();
    }

    private void stubNewContributor() {
        when(eventContributorRepository.findByFloodEventIdAndUserId(any(), any()))
                .thenReturn(Optional.empty());
    }

    private void stubHandleNewReport(FloodEvent saved) {
        when(floodEventRepository.save(any())).thenReturn(saved);
        when(trustScoreRepository.save(any())).thenReturn(TrustScore.builder().build());
        stubNewContributor();
    }

    @Test
    void handleNewReport_scoreRejected_savesRejectedEvent() {
        stubHandleNewReport(buildSavedEvent("REJECTED"));
        service.handleNewReport(buildMsg(), 0.1);
    }

    @Test
    void handleNewReport_scorePending_savesPendingEvent() {
        stubHandleNewReport(buildSavedEvent("PENDING"));
        service.handleNewReport(buildMsg(), 0.5);
    }

    @Test
    void handleNewReport_scoreActive_savesActiveEvent() {
        stubHandleNewReport(buildSavedEvent("ACTIVE"));
        service.handleNewReport(buildMsg(), 0.9);
    }

    @Test
    void handleNewReport_savesContributorWithReportIdFromMessage() {
        stubHandleNewReport(buildSavedEvent("ACTIVE"));

        service.handleNewReport(buildMsg(), 0.9);

        ArgumentCaptor<EventContributor> captor = ArgumentCaptor.forClass(EventContributor.class);
        verify(eventContributorRepository).save(captor.capture());
        assertEquals("report-001", captor.getValue().getReportId());
    }

    @Test
    void handleNewReport_duplicateContributor_skipsContributorSave() {
        when(floodEventRepository.save(any())).thenReturn(buildSavedEvent("PENDING"));
        when(trustScoreRepository.save(any())).thenReturn(TrustScore.builder().build());
        when(eventContributorRepository.findByFloodEventIdAndUserId(any(), any()))
                .thenReturn(Optional.of(EventContributor.builder().build()));
        service.handleNewReport(buildMsg(), 0.5);
    }

    @Test
    void handleNewReport_contributorSaveThrows_continuesWithoutException() {
        when(floodEventRepository.save(any())).thenReturn(buildSavedEvent("PENDING"));
        when(trustScoreRepository.save(any())).thenReturn(TrustScore.builder().build());
        stubNewContributor();
        when(eventContributorRepository.save(any())).thenThrow(new RuntimeException("DB error"));
        service.handleNewReport(buildMsg(), 0.5);
    }

    @Test
    void handleNewReport_repositoryThrows_throwsAppException() {
        when(floodEventRepository.save(any())).thenThrow(new RuntimeException("DB down"));
        assertThrows(AppException.class, () -> service.handleNewReport(buildMsg(), 0.5));
    }

    @Test
    void handleNewReport_objectMapperThrows_continuesWithEmptyJson() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("json error"));
        stubHandleNewReport(buildSavedEvent("PENDING"));
        service.handleNewReport(buildMsg(), 0.5);
    }

    @Test
    void handleClusterUpdate_scoreBelowActive_staysPending() {
        FloodEvent existing = buildSavedEvent("PENDING");
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.of(existing));
        when(floodEventRepository.save(any())).thenReturn(existing);
        when(trustScoreRepository.save(any())).thenReturn(TrustScore.builder().build());
        stubNewContributor();
        service.handleClusterUpdate(buildMsg(), "evt-001", 0.5);
    }

    @Test
    void handleClusterUpdate_scoreReachesThreshold_transitionsToActive() {
        FloodEvent existing = buildSavedEvent("PENDING");
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.of(existing));
        when(floodEventRepository.save(any())).thenReturn(existing);
        when(trustScoreRepository.save(any())).thenReturn(TrustScore.builder().build());
        stubNewContributor();
        service.handleClusterUpdate(buildMsg(), "evt-001", 0.9);
    }

    @Test
    void handleClusterUpdate_withImageUrl_rawDataNull_updatesRawData() {
        FloodEvent existing = buildSavedEvent("PENDING");
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.of(existing));
        when(floodEventRepository.save(any())).thenReturn(existing);
        when(trustScoreRepository.save(any())).thenReturn(TrustScore.builder().build());
        stubNewContributor();
        service.handleClusterUpdate(buildMsg("http://image.url/flood.jpg"), "evt-001", 0.5);
    }

    @Test
    void handleClusterUpdate_withImageUrl_rawDataNotNull_updatesRawData() {
        FloodEvent existing = buildSavedEventWithRawData("PENDING");
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.of(existing));
        when(floodEventRepository.save(any())).thenReturn(existing);
        when(trustScoreRepository.save(any())).thenReturn(TrustScore.builder().build());
        stubNewContributor();
        service.handleClusterUpdate(buildMsg("http://image.url/flood.jpg"), "evt-001", 0.5);
    }

    @Test
    void handleClusterUpdate_eventNotFound_throwsAppException() {
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.handleClusterUpdate(buildMsg(), "evt-001", 0.5));
    }

    @Test
    void handleClusterUpdate_saveThrows_throwsAppException() {
        FloodEvent existing = buildSavedEvent("PENDING");
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.of(existing));
        when(floodEventRepository.save(any())).thenThrow(new RuntimeException("DB down"));
        assertThrows(AppException.class, () -> service.handleClusterUpdate(buildMsg(), "evt-001", 0.5));
    }

    @Test
    void getEventStatus_eventFound_returnsStatus() {
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.of(buildSavedEvent("ACTIVE")));
        service.getEventStatus("evt-001");
    }

    @Test
    void getEventStatus_eventNotFound_throwsAppException() {
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.getEventStatus("evt-001"));
    }

    @Test
    void handleFastUpdate_eventFound_updatesVoteCount() {
        FloodEvent existing = buildSavedEvent("ACTIVE");
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.of(existing));
        stubNewContributor();
        when(floodEventRepository.save(any())).thenReturn(existing);
        service.handleFastUpdate(buildMsg(), "evt-001");
    }

    @Test
    void handleFastUpdate_withImageUrl_rawDataNull_updatesRawData() {
        FloodEvent existing = buildSavedEvent("ACTIVE");
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.of(existing));
        stubNewContributor();
        when(floodEventRepository.save(any())).thenReturn(existing);
        service.handleFastUpdate(buildMsg("http://image.url/flood.jpg"), "evt-001");
    }

    @Test
    void handleFastUpdate_withImageUrl_rawDataNotNull_updatesRawData() {
        FloodEvent existing = buildSavedEventWithRawData("ACTIVE");
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.of(existing));
        stubNewContributor();
        when(floodEventRepository.save(any())).thenReturn(existing);
        service.handleFastUpdate(buildMsg("http://image.url/flood.jpg"), "evt-001");
    }

    @Test
    void handleFastUpdate_eventNotFound_throwsAppException() {
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.handleFastUpdate(buildMsg(), "evt-001"));
    }

    @Test
    void handleFastUpdate_saveThrows_throwsAppException() {
        FloodEvent existing = buildSavedEvent("ACTIVE");
        when(floodEventRepository.findByEventId("evt-001")).thenReturn(Optional.of(existing));
        stubNewContributor();
        when(floodEventRepository.save(any())).thenThrow(new RuntimeException("DB error"));
        assertThrows(AppException.class, () -> service.handleFastUpdate(buildMsg(), "evt-001"));
    }

    @Test
    void getContributorsByEventId_success_returnsList() {
        UUID eventId = UUID.randomUUID();
        when(eventContributorRepository.findByFloodEventIdOrderByCreatedAtDesc(eventId)).thenReturn(List.of());
        service.getContributorsByEventId(eventId);
    }

    @Test
    void getContributorsByEventId_throws_throwsAppException() {
        UUID eventId = UUID.randomUUID();
        when(eventContributorRepository.findByFloodEventIdOrderByCreatedAtDesc(eventId))
                .thenThrow(new RuntimeException("DB error"));
        assertThrows(AppException.class, () -> service.getContributorsByEventId(eventId));
    }
}
