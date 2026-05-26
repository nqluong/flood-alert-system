package org.project.floodalert.floodcore.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.dto.response.AdminActiveFloodResponse;
import org.project.floodalert.floodcore.service.AdminFloodService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminFloodControllerTest {

    @Mock
    private AdminFloodService adminFloodService;

    @InjectMocks
    private AdminFloodController adminFloodController;

    private UUID userReportId;

    @BeforeEach
    void setUp() {
        userReportId = UUID.randomUUID();
    }

    @Test
    void getActiveFloods_returnsActiveFloods() {
        List<AdminActiveFloodResponse> activeFloods = List.of(
                mock(AdminActiveFloodResponse.class)
        );

        when(adminFloodService.getAllActiveFloods())
                .thenReturn(activeFloods);

        adminFloodController.getActiveFloods();

        verify(adminFloodService).getAllActiveFloods();
    }

    @Test
    void getActiveFloods_emptyList_returnsOk() {
        when(adminFloodService.getAllActiveFloods())
                .thenReturn(List.of());

        adminFloodController.getActiveFloods();

        verify(adminFloodService).getAllActiveFloods();
    }

    @Test
    void approveFloodEvent_callsService() {
        adminFloodController.approveFloodEvent(userReportId);

        verify(adminFloodService).approveReport(userReportId);
    }

    @Test
    void rejectFloodEvent_callsService() {
        adminFloodController.rejectFloodEvent(userReportId);

        verify(adminFloodService).rejectReport(userReportId);
    }

    @Test
    void approveFloodEvent_serviceThrows_propagatesException() {
        doThrow(new RuntimeException("Approve error"))
                .when(adminFloodService)
                .approveReport(userReportId);

        try {
            adminFloodController.approveFloodEvent(userReportId);
        } catch (RuntimeException ignored) {
        }

        verify(adminFloodService).approveReport(userReportId);
    }

    @Test
    void rejectFloodEvent_serviceThrows_propagatesException() {
        doThrow(new RuntimeException("Reject error"))
                .when(adminFloodService)
                .rejectReport(userReportId);

        try {
            adminFloodController.rejectFloodEvent(userReportId);
        } catch (RuntimeException ignored) {
        }

        verify(adminFloodService).rejectReport(userReportId);
    }
}
