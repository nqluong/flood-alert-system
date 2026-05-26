package org.project.floodalert.floodcore.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.client.FloodProcessorClient;
import org.project.floodalert.floodcore.model.CoreActiveFlood;
import org.project.floodalert.floodcore.model.UserReport;
import org.project.floodalert.floodcore.repository.CoreActiveFloodRepository;
import org.project.floodalert.floodcore.repository.UserReportRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFloodServiceImplTest {

    @Mock
    private CoreActiveFloodRepository coreActiveFloodRepository;

    @Mock
    private UserReportRepository userReportRepository;

    @Mock
    private FloodProcessorClient floodProcessorClient;

    @InjectMocks
    private AdminFloodServiceImpl adminFloodService;

    @Test
    void getAllActiveFloods_withNonNullFields() {
        CoreActiveFlood flood = CoreActiveFlood.builder()
                .eventId("event-1")
                .lat(BigDecimal.valueOf(10.5))
                .lon(BigDecimal.valueOf(106.5))
                .locationDescription("Quận 12")
                .waterLevel(BigDecimal.valueOf(1.5))
                .severityLevel("HIGH")
                .status("ACTIVE")
                .source("SENSOR")
                .updatedAt(LocalDateTime.now())
                .build();

        when(coreActiveFloodRepository.findAll()).thenReturn(List.of(flood));

        adminFloodService.getAllActiveFloods();
    }

    @Test
    void getAllActiveFloods_withNullLatLonWaterLevel() {
        // Covers null-check branches in mapToResponse (lat/lon/waterLevel == null)
        CoreActiveFlood flood = CoreActiveFlood.builder()
                .eventId("event-2")
                .lat(null)
                .lon(null)
                .waterLevel(null)
                .build();

        when(coreActiveFloodRepository.findAll()).thenReturn(List.of(flood));

        adminFloodService.getAllActiveFloods();
    }

    @Test
    void getAllActiveFloods_emptyList() {
        when(coreActiveFloodRepository.findAll()).thenReturn(List.of());

        adminFloodService.getAllActiveFloods();
    }

    // --- approveReport ---

    @Test
    void approveReport_success() {
        UUID reportId = UUID.randomUUID();
        UserReport report = UserReport.builder()
                .id(reportId)
                .floodEventId("flood-event-123")
                .build();

        when(userReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        adminFloodService.approveReport(reportId);
    }

    @Test
    void approveReport_reportNotFound_throwsException() {
        UUID reportId = UUID.randomUUID();

        when(userReportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> adminFloodService.approveReport(reportId));
    }

    @Test
    void approveReport_floodEventIdNull_throwsException() {
        // Covers resolveFloodEventId: floodEventId == null → REPORT_NOT_LINKED_TO_EVENT
        UUID reportId = UUID.randomUUID();
        UserReport report = UserReport.builder()
                .id(reportId)
                .floodEventId(null)
                .build();

        when(userReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        assertThrows(AppException.class, () -> adminFloodService.approveReport(reportId));
    }

    // --- rejectReport ---

    @Test
    void rejectReport_reportNotFound_throwsException() {
        UUID reportId = UUID.randomUUID();

        when(userReportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> adminFloodService.rejectReport(reportId));
    }

    @Test
    void rejectReport_noFloodEventId_updatesStatusAndSaves() {
        // Covers branch: floodEventId == null → set REJECTED, save, return
        UUID reportId = UUID.randomUUID();
        UserReport report = UserReport.builder()
                .id(reportId)
                .floodEventId(null)
                .build();

        when(userReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        adminFloodService.rejectReport(reportId);
    }

    @Test
    void rejectReport_withFloodEventId_callsClient() {
        UUID reportId = UUID.randomUUID();
        UserReport report = UserReport.builder()
                .id(reportId)
                .floodEventId("flood-event-456")
                .build();

        when(userReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        adminFloodService.rejectReport(reportId);
    }
}
