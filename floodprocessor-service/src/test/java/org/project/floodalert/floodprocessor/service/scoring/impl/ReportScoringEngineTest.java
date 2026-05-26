package org.project.floodalert.floodprocessor.service.scoring.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.service.scoring.ReportScoringStrategy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportScoringEngineTest {

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private ReportMessage buildMsg(String imageUrl) {
        return ReportMessage.builder()
                .reportId("report-001")
                .userId(UUID.randomUUID())
                .imageUrl(imageUrl)
                .build();
    }

    private ReportScoringStrategy mockStrategy(String name, double score) {
        ReportScoringStrategy s = mock(ReportScoringStrategy.class);
        when(s.isApplicable(any())).thenReturn(true);
        when(s.getStrategyName()).thenReturn(name);
        when(s.calculateScore(any())).thenReturn(score);
        return s;
    }

    private ReportScoringStrategy mockFailingStrategy(String name) {
        ReportScoringStrategy s = mock(ReportScoringStrategy.class);
        when(s.isApplicable(any())).thenReturn(true);
        when(s.getStrategyName()).thenReturn(name);
        when(s.calculateScore(any())).thenThrow(new RuntimeException("Strategy error"));
        return s;
    }

    @Test
    void evaluateScore_emptyStrategyList_returnsTotalScoreZero() {
        ReportScoringEngine engine = new ReportScoringEngine(List.of(), executor);
        engine.evaluateScore(buildMsg("http://img.url/flood.jpg"));
    }

    @Test
    void evaluateScore_noApplicableStrategies_returnsTotalScoreZero() {
        ReportScoringStrategy strategy = mock(ReportScoringStrategy.class);
        when(strategy.isApplicable(any())).thenReturn(false);
        ReportScoringEngine engine = new ReportScoringEngine(List.of(strategy), executor);
        engine.evaluateScore(buildMsg("http://img.url/flood.jpg"));
    }

    @Test
    void evaluateScore_allStrategiesSucceed_withImage_computesWeightedScore() {
        ReportScoringStrategy ai = mockStrategy("AI_VISION", 0.8);
        ReportScoringStrategy spatial = mockStrategy("SPATIAL_CONSENSUS", 0.6);
        ReportScoringStrategy reputation = mockStrategy("USER_REPUTATION", 0.7);
        ReportScoringEngine engine = new ReportScoringEngine(List.of(ai, spatial, reputation), executor);
        engine.evaluateScore(buildMsg("http://img.url/flood.jpg"));
    }

    @Test
    void evaluateScore_allStrategiesSucceed_withoutImage_computesWeightedScore() {
        ReportScoringStrategy spatial = mockStrategy("SPATIAL_CONSENSUS", 0.6);
        ReportScoringStrategy reputation = mockStrategy("USER_REPUTATION", 0.7);
        ReportScoringEngine engine = new ReportScoringEngine(List.of(spatial, reputation), executor);
        engine.evaluateScore(buildMsg(null));
    }

    @Test
    void evaluateScore_oneStrategyFails_redistributesWeights() {
        ReportScoringStrategy failing = mockFailingStrategy("AI_VISION");
        ReportScoringStrategy spatial = mockStrategy("SPATIAL_CONSENSUS", 0.6);
        ReportScoringEngine engine = new ReportScoringEngine(List.of(failing, spatial), executor);
        engine.evaluateScore(buildMsg("http://img.url/flood.jpg"));
    }

    @Test
    void evaluateScore_allStrategiesFail_returnsTotalScoreZero() {
        ReportScoringStrategy ai = mockFailingStrategy("AI_VISION");
        ReportScoringStrategy spatial = mockFailingStrategy("SPATIAL_CONSENSUS");
        ReportScoringEngine engine = new ReportScoringEngine(List.of(ai, spatial), executor);
        engine.evaluateScore(buildMsg("http://img.url/flood.jpg"));
    }

    @Test
    void evaluateScore_unknownStrategyName_returnsZeroScore() {
        ReportScoringStrategy unknown = mockStrategy("UNKNOWN_STRATEGY", 0.5);
        ReportScoringEngine engine = new ReportScoringEngine(List.of(unknown), executor);
        engine.evaluateScore(buildMsg("http://img.url/flood.jpg"));
    }

    @Test
    void evaluateTotalScore_delegatesToEvaluateScore() {
        ReportScoringStrategy spatial = mockStrategy("SPATIAL_CONSENSUS", 0.5);
        ReportScoringEngine engine = new ReportScoringEngine(List.of(spatial), executor);
        engine.evaluateTotalScore(buildMsg(null));
    }
}
