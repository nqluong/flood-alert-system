package org.project.floodalert.floodcore.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.dto.event.UserReportEvent;
import org.project.floodalert.floodcore.dto.request.UserReportFilterRequest;
import org.project.floodalert.floodcore.dto.request.UserReportRequest;
import org.project.floodalert.floodcore.dto.response.UserReportResponse;
import org.project.floodalert.floodcore.kafka.UserReportEventProducer;
import org.project.floodalert.floodcore.mapper.UserReportMapper;
import org.project.floodalert.floodcore.model.UserReport;
import org.project.floodalert.floodcore.repository.UserReportRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserReportServiceImplTest {

    @Mock private UserReportRepository userReportRepository;
    @Mock private UserReportMapper userReportMapper;
    @Mock private UserReportEventProducer userReportEventProducer;

    @InjectMocks
    private UserReportServiceImpl userReportService;

    private UserReportRequest buildRequest(Double lat, Double lon) {
        return UserReportRequest.builder().lat(lat).lon(lon).build();
    }

    private UserReportFilterRequest buildFilter(String sortDirection) {
        return UserReportFilterRequest.builder()
                .page(0).size(20).sortBy("createdAt").sortDirection(sortDirection)
                .build();
    }

    // --- submitUserReport ---

    @Test
    void submitUserReport_success() {
        UserReport report = UserReport.builder().build();
        when(userReportMapper.toEntity(any(), any(), anyString())).thenReturn(report);
        when(userReportRepository.save(any())).thenReturn(report);
        when(userReportMapper.toEvent(any(), any())).thenReturn(new UserReportEvent());
        when(userReportMapper.toResponse(any())).thenReturn(new UserReportResponse());
        userReportService.submitUserReport(buildRequest(10.5, 106.5), UUID.randomUUID());
    }

    @Test
    void submitUserReport_nullLat_throwsAppException() {
        assertThrows(AppException.class, () ->
                userReportService.submitUserReport(buildRequest(null, 106.5), UUID.randomUUID()));
    }

    @Test
    void submitUserReport_nullLon_throwsAppException() {
        assertThrows(AppException.class, () ->
                userReportService.submitUserReport(buildRequest(10.5, null), UUID.randomUUID()));
    }

    @Test
    void submitUserReport_latOutOfRange_throwsAppException() {
        assertThrows(AppException.class, () ->
                userReportService.submitUserReport(buildRequest(-91.0, 106.5), UUID.randomUUID()));
    }

    @Test
    void submitUserReport_latTooLarge_throwsAppException() {
        assertThrows(AppException.class, () ->
                userReportService.submitUserReport(buildRequest(91.0, 106.5), UUID.randomUUID()));
    }

    @Test
    void submitUserReport_lonOutOfRange_throwsAppException() {
        assertThrows(AppException.class, () ->
                userReportService.submitUserReport(buildRequest(10.5, 181.0), UUID.randomUUID()));
    }

    @Test
    void submitUserReport_lonTooSmall_throwsAppException() {
        assertThrows(AppException.class, () ->
                userReportService.submitUserReport(buildRequest(10.5, -181.0), UUID.randomUUID()));
    }

    @Test
    void submitUserReport_systemExceptionWrapped() {
        when(userReportMapper.toEntity(any(), any(), anyString())).thenReturn(UserReport.builder().build());
        doThrow(new RuntimeException("DB error")).when(userReportRepository).save(any());
        assertThrows(AppException.class, () ->
                userReportService.submitUserReport(buildRequest(10.5, 106.5), UUID.randomUUID()));
    }

    // --- getReportsByUser ---

    @Test
    void getReportsByUser_sortASC_emptyResult() {
        when(userReportRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        userReportService.getReportsByUser(UUID.randomUUID(), buildFilter("ASC"));
    }

    @Test
    void getReportsByUser_sortDESC_withContent() {
        when(userReportRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(UserReport.builder().build())));
        when(userReportMapper.toSummaryResponse(any())).thenReturn(new UserReportResponse());
        userReportService.getReportsByUser(UUID.randomUUID(), buildFilter("DESC"));
    }

    // --- getAllReports ---

    @Test
    void getAllReports_sortASC_emptyResult() {
        when(userReportRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        userReportService.getAllReports(buildFilter("ASC"));
    }

    @Test
    void getAllReports_sortDESC_withContent() {
        when(userReportRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(UserReport.builder().build())));
        when(userReportMapper.toSummaryResponse(any())).thenReturn(new UserReportResponse());
        userReportService.getAllReports(buildFilter("DESC"));
    }
}
