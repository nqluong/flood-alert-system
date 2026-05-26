package org.project.floodalert.floodprocessor.messaging.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.request.UserReportEvent;
import org.project.floodalert.floodprocessor.service.processing.ReportProcessingUseCase;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserReportListenerTest {
    @Mock
    ReportProcessingUseCase reportProcessingUseCase;
    @Mock
    Acknowledgment acknowledgment;
    @InjectMocks
    UserReportListener userReportListener;

    @Test
    void userReport_success_acknowledges() {
        UserReportEvent event = mockUserReportEvent();
        userReportListener.consume(event, acknowledgment);

        verify(reportProcessingUseCase).process(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void userReport_processingThrows_stillAcknowledges() {
        UserReportEvent event = mockUserReportEvent();
        doThrow(new RuntimeException("processing error")).when(reportProcessingUseCase).process(event);

        userReportListener.consume(event, acknowledgment);

        verify(acknowledgment).acknowledge();
    }
    private UserReportEvent mockUserReportEvent() {
        UserReportEvent event = mock(UserReportEvent.class);
        when(event.getReportId()).thenReturn("report-1");
        when(event.getUserId()).thenReturn(java.util.UUID.randomUUID());
        when(event.getLat()).thenReturn(10.0);
        when(event.getLon()).thenReturn(106.0);
        return event;
    }
}
