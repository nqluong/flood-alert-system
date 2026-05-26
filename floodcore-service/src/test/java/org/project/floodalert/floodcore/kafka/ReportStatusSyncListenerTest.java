package org.project.floodalert.floodcore.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.dto.event.ReportStatusUpdateEvent;
import org.project.floodalert.floodcore.model.UserReport;
import org.project.floodalert.floodcore.repository.UserReportRepository;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportStatusSyncListenerTest {

    @Mock
    private UserReportRepository userReportRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private ReportStatusSyncListener listener;

    private ReportStatusUpdateEvent event;
    private UserReport report;
    private String reportId;
    private String eventId;

    @BeforeEach
    void setUp() {
        reportId = String.valueOf(UUID.randomUUID());
        eventId = String.valueOf(UUID.randomUUID());

        event = new ReportStatusUpdateEvent();
        event.setReportId(reportId);
        event.setStatus("APPROVED");
        event.setEventId(eventId);
        event.setScore(90.0);
        event.setAiScore(85.0);
        event.setSpatialScore(80.0);
        event.setReputationScore(95.0);
        event.setRejectReason("Ảnh không hợp lệ");

        report = new UserReport();
        report.setReportId(reportId);
        report.setStatus("PENDING");
    }

    @Test
    void handleReportStatusUpdate_reportNotFound_acknowledgesAndReturns() {
        when(userReportRepository.findByReportId(reportId))
                .thenReturn(Optional.empty());

        listener.handleReportStatusUpdate(event, acknowledgment);

        verify(userReportRepository).findByReportId(reportId);
        verify(userReportRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleReportStatusUpdate_approved_updatesReportAndAcknowledges() {
        when(userReportRepository.findByReportId(reportId))
                .thenReturn(Optional.of(report));

        listener.handleReportStatusUpdate(event, acknowledgment);

        verify(userReportRepository).findByReportId(reportId);
        verify(userReportRepository).save(report);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleReportStatusUpdate_rejected_updatesReportAndAcknowledges() {
        event.setStatus("REJECTED");

        when(userReportRepository.findByReportId(reportId))
                .thenReturn(Optional.of(report));

        listener.handleReportStatusUpdate(event, acknowledgment);

        verify(userReportRepository).findByReportId(reportId);
        verify(userReportRepository).save(report);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleReportStatusUpdate_eventIdNull_stillSavesAndAcknowledges() {
        event.setEventId(null);

        when(userReportRepository.findByReportId(reportId))
                .thenReturn(Optional.of(report));

        listener.handleReportStatusUpdate(event, acknowledgment);

        verify(userReportRepository).findByReportId(reportId);
        verify(userReportRepository).save(report);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleReportStatusUpdate_reportAlreadyHasFloodEventId_doesNotFail() {
        report.setFloodEventId(String.valueOf(UUID.randomUUID()));

        when(userReportRepository.findByReportId(reportId))
                .thenReturn(Optional.of(report));

        listener.handleReportStatusUpdate(event, acknowledgment);

        verify(userReportRepository).findByReportId(reportId);
        verify(userReportRepository).save(report);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleReportStatusUpdate_scoresNull_stillSavesAndAcknowledges() {
        event.setScore(null);
        event.setAiScore(null);
        event.setSpatialScore(null);
        event.setReputationScore(null);

        when(userReportRepository.findByReportId(reportId))
                .thenReturn(Optional.of(report));

        listener.handleReportStatusUpdate(event, acknowledgment);

        verify(userReportRepository).findByReportId(reportId);
        verify(userReportRepository).save(report);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleReportStatusUpdate_otherStatus_stillSavesAndAcknowledges() {
        event.setStatus("PENDING_REVIEW");

        when(userReportRepository.findByReportId(reportId))
                .thenReturn(Optional.of(report));

        listener.handleReportStatusUpdate(event, acknowledgment);

        verify(userReportRepository).findByReportId(reportId);
        verify(userReportRepository).save(report);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleReportStatusUpdate_repositoryThrows_throwsExceptionAndDoesNotAcknowledge() {
        when(userReportRepository.findByReportId(reportId))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> listener.handleReportStatusUpdate(event, acknowledgment));

        verify(userReportRepository).findByReportId(reportId);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void handleReportStatusUpdate_saveThrows_throwsExceptionAndDoesNotAcknowledge() {
        when(userReportRepository.findByReportId(reportId))
                .thenReturn(Optional.of(report));

        when(userReportRepository.save(report))
                .thenThrow(new RuntimeException("Save error"));

        assertThrows(RuntimeException.class,
                () -> listener.handleReportStatusUpdate(event, acknowledgment));

        verify(userReportRepository).findByReportId(reportId);
        verify(userReportRepository).save(report);
        verify(acknowledgment, never()).acknowledge();
    }
}