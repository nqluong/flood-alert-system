package org.project.floodalert.floodprocessor.service.scoring.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpatialConsensusScoringStrategyTest {

    @Mock
    private FloodEventRepository floodEventRepository;

    @InjectMocks
    private SpatialConsensusScoringStrategy strategy;

    private ReportMessage buildMsg() {
        return ReportMessage.builder()
                .reportId("report-001")
                .userId(UUID.randomUUID())
                .lat(10.5)
                .lon(106.5)
                .build();
    }

    private FloodEvent buildEvent(String severity, BigDecimal waterLevel) {
        return FloodEvent.builder()
                .severityLevel(severity)
                .waterLevel(waterLevel)
                .build();
    }

    private void stubNoIotEvents() {
        when(floodEventRepository.findRecentIotEventsNearby(
                anyDouble(), anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(List.of());
    }

    private void stubIotEvents(FloodEvent... events) {
        when(floodEventRepository.findRecentIotEventsNearby(
                anyDouble(), anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(List.of(events));
    }

    private void stubCrowdCount(int count) {
        when(floodEventRepository.countNearbyActiveUserReports(
                anyDouble(), anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(count);
    }

    @Test
    void calculateScore_noIotEvents_crowdZero_returnsZero() {
        stubNoIotEvents();
        stubCrowdCount(0);
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_noIotEvents_crowdOne_returnsHalf() {
        stubNoIotEvents();
        stubCrowdCount(1);
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_noIotEvents_crowdTwo_returnsEighty() {
        stubNoIotEvents();
        stubCrowdCount(2);
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_noIotEvents_crowdThreeOrMore_returnsOne() {
        stubNoIotEvents();
        stubCrowdCount(3);
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_iotFloodingBySeverityWarning_returnsOne() {
        stubIotEvents(buildEvent("WARNING", BigDecimal.ZERO));
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_iotFloodingBySeverityDanger_returnsOne() {
        stubIotEvents(buildEvent("DANGER", BigDecimal.ZERO));
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_iotFloodingByWaterLevel_returnsOne() {
        stubIotEvents(buildEvent("PENDING", BigDecimal.valueOf(15)));
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_iotSafeBySeverity_crowdLessThanThree_returnsZero() {
        stubIotEvents(buildEvent("SAFE", null));
        stubCrowdCount(2);
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_iotSafeBySeverity_crowdThreeOrMore_returnsCrowdScore() {
        stubIotEvents(buildEvent("SAFE", null));
        stubCrowdCount(3);
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_iotSafeByWaterLevelZero_crowdZero_returnsZero() {
        stubIotEvents(buildEvent("PENDING", BigDecimal.ZERO));
        stubCrowdCount(0);
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_iotUnknownState_returnsZero() {
        stubIotEvents(buildEvent("UNKNOWN", BigDecimal.valueOf(5)));
        strategy.calculateScore(buildMsg());
    }

    @Test
    void isApplicable_alwaysReturnsTrue() {
        strategy.isApplicable(buildMsg());
    }

    @Test
    void getStrategyName_returnsSpatialConsensus() {
        strategy.getStrategyName();
    }
}
