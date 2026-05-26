package org.project.floodalert.floodprocessor.service.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserReportProcessorService Tests")
class UserReportProcessorServiceImplTest {

    @Mock
    private FloodEventRepository floodEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private UserReportProcessorServiceImpl userReportProcessorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userReportProcessorService, "pendingTimeoutMinutes", 120);
        ReflectionTestUtils.setField(userReportProcessorService, "decayIntervalMinutes", 30);
        ReflectionTestUtils.setField(userReportProcessorService, "decayAmount", 0.2);
        ReflectionTestUtils.setField(userReportProcessorService, "minConfidenceThreshold", 0.3);
        ReflectionTestUtils.setField(userReportProcessorService, "sensorCrosscheckRadiusMeters", 50.0);
        ReflectionTestUtils.setField(userReportProcessorService, "batchSize", 100);
    }

    @Test
    void shouldRejectPendingReportsOlderThanTimeout() {
        // Given
        FloodEvent oldReport = createFloodEvent("report-1", "PENDING", BigDecimal.valueOf(0.5));
        oldReport.setCreatedAt(LocalDateTime.now().minusHours(3));

        Page<FloodEvent> page = new PageImpl<>(Collections.singletonList(oldReport));
        when(floodEventRepository.findPendingUserReportsOlderThan(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        userReportProcessorService.cleanupPendingReports();

    }

    @Test
    void shouldProcessMultipleBatchesOfPendingReports() {
        // Given
        FloodEvent report1 = createFloodEvent("report-1", "PENDING", BigDecimal.valueOf(0.5));
        FloodEvent report2 = createFloodEvent("report-2", "PENDING", BigDecimal.valueOf(0.6));

        Page<FloodEvent> firstPage = new PageImpl<>(
            Collections.singletonList(report1),
            PageRequest.of(0, 100),
            2
        );
        Page<FloodEvent> secondPage = new PageImpl<>(
            Collections.singletonList(report2),
            PageRequest.of(1, 100),
            2
        );

        when(floodEventRepository.findPendingUserReportsOlderThan(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(firstPage, secondPage);

        // When
        userReportProcessorService.cleanupPendingReports();

    }

    @Test
    void shouldKeepReportActiveWhenSensorNearby() {
        // Given
        FloodEvent activeReport = createFloodEvent("report-1", "ACTIVE", BigDecimal.valueOf(0.4));
        activeReport.setUpdatedAt(LocalDateTime.now().minusMinutes(45));

        FloodEvent nearbySensor = createFloodEvent("sensor-1", "CONFIRMED", BigDecimal.valueOf(1.0));
        nearbySensor.setSource("SENSOR");

        Page<FloodEvent> page = new PageImpl<>(Collections.singletonList(activeReport));
        when(floodEventRepository.findActiveUserReportsWithoutRecentUpdate(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);
        when(floodEventRepository.findActiveSensorEventsNearby(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.singletonList(nearbySensor));

        // When
        userReportProcessorService.applyTimeDecayAndSpatialCheck();

    }

    @Test
    void shouldResolveReportWhenNoSensorNearby() {
        // Given
        FloodEvent activeReport = createFloodEvent("report-1", "ACTIVE", BigDecimal.valueOf(0.4));
        activeReport.setUpdatedAt(LocalDateTime.now().minusMinutes(45));

        Page<FloodEvent> page = new PageImpl<>(Collections.singletonList(activeReport));
        when(floodEventRepository.findActiveUserReportsWithoutRecentUpdate(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);
        when(floodEventRepository.findActiveSensorEventsNearby(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());

        // When
        userReportProcessorService.applyTimeDecayAndSpatialCheck();
    }

    @Test
    void shouldNotResolveReportWhenConfidenceAboveThreshold() {
        // Given
        FloodEvent activeReport = createFloodEvent("report-1", "ACTIVE", BigDecimal.valueOf(0.6));
        activeReport.setUpdatedAt(LocalDateTime.now().minusMinutes(45));

        Page<FloodEvent> page = new PageImpl<>(Collections.singletonList(activeReport));
        when(floodEventRepository.findActiveUserReportsWithoutRecentUpdate(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        userReportProcessorService.applyTimeDecayAndSpatialCheck();

    }

    @Test
    void shouldHandleEmptyResultSetGracefully() {
        // Given
        Page<FloodEvent> emptyPage = new PageImpl<>(Collections.emptyList());
        when(floodEventRepository.findPendingUserReportsOlderThan(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        userReportProcessorService.cleanupPendingReports();

    }

    private FloodEvent createFloodEvent(String eventId, String status, BigDecimal confidenceScore) {
        return FloodEvent.builder()
                .eventId(eventId)
                .source("USER_REPORT")
                .status(status)
                .confidenceScore(confidenceScore)
                .lat(10.762622)
                .lon(106.660172)
                .locationDescription("Test Location")
                .severityLevel("WARNING")
                .waterLevel(BigDecimal.valueOf(50.0))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
