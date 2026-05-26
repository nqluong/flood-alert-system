package org.project.floodalert.floodcore.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.common.security.SecurityContextUtils;
import org.project.floodalert.floodcore.dto.request.UserReportFilterRequest;
import org.project.floodalert.floodcore.dto.request.UserReportRequest;
import org.project.floodalert.floodcore.dto.response.UserReportResponse;
import org.project.floodalert.floodcore.service.UserReportService;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserReportControllerTest {

    @Mock
    private UserReportService userReportService;

    @InjectMocks
    private UserReportController userReportController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void submitReport_callsService() {
        UserReportRequest request = mock(UserReportRequest.class);
        UserReportResponse response = mock(UserReportResponse.class);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(userReportService.submitUserReport(request, userId))
                    .thenReturn(response);

            userReportController.submitReport(request);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(userReportService).submitUserReport(request, userId);
        }
    }

    @Test
    void getMyReports_callsService() {
        UserReportFilterRequest filter = mock(UserReportFilterRequest.class);
        PageResponse<UserReportResponse> response = mock(PageResponse.class);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(userReportService.getReportsByUser(userId, filter))
                    .thenReturn(response);

            userReportController.getMyReports(filter);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(userReportService).getReportsByUser(userId, filter);
        }
    }

    @Test
    void getAllReports_callsService() {
        UserReportFilterRequest filter = mock(UserReportFilterRequest.class);
        PageResponse<UserReportResponse> response = mock(PageResponse.class);

        when(userReportService.getAllReports(filter))
                .thenReturn(response);

        userReportController.getAllReports(filter);

        verify(userReportService).getAllReports(filter);
    }

    @Test
    void getReportsByUser_callsService() {
        UserReportFilterRequest filter = mock(UserReportFilterRequest.class);
        PageResponse<UserReportResponse> response = mock(PageResponse.class);

        when(userReportService.getReportsByUser(userId, filter))
                .thenReturn(response);

        userReportController.getReportsByUser(userId, filter);

        verify(userReportService).getReportsByUser(userId, filter);
    }

    @Test
    void submitReport_serviceThrows_stillCallsService() {
        UserReportRequest request = mock(UserReportRequest.class);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(userReportService.submitUserReport(request, userId))
                    .thenThrow(new RuntimeException("Submit error"));

            try {
                userReportController.submitReport(request);
            } catch (RuntimeException ignored) {
            }

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(userReportService).submitUserReport(request, userId);
        }
    }
}