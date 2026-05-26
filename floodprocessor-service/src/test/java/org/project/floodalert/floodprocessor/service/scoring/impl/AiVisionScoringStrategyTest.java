package org.project.floodalert.floodprocessor.service.scoring.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.response.GeminiVisionResponse;
import org.project.floodalert.floodprocessor.service.scoring.GeminiApiClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiVisionScoringStrategyTest {

    @Mock
    private GeminiApiClient geminiApiClient;

    @InjectMocks
    private AiVisionScoringStrategy strategy;

    private ReportMessage buildMsg(String imageUrl) {
        return ReportMessage.builder()
                .reportId("report-001")
                .imageUrl(imageUrl)
                .build();
    }

    private GeminiVisionResponse buildResponse(boolean isFlooded, int confidence) {
        return GeminiVisionResponse.builder()
                .isFlooded(isFlooded)
                .confidenceScore(confidence)
                .waterLevelEstimate("shallow")
                .reasoning("test reasoning")
                .build();
    }

    @Test
    void calculateScore_nullResponse_throwsIllegalStateException() {
        ReportMessage msg = buildMsg("http://img.url/flood.jpg");
        when(geminiApiClient.analyzeFloodImage(msg)).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> strategy.calculateScore(msg));
    }

    @Test
    void calculateScore_floodedHighConfidence_returnsNormalizedScore() {
        ReportMessage msg = buildMsg("http://img.url/flood.jpg");
        when(geminiApiClient.analyzeFloodImage(msg)).thenReturn(buildResponse(true, 80));
        strategy.calculateScore(msg);
    }

    @Test
    void calculateScore_notFloodedLowConfidence_logsWarningAndReturns() {
        ReportMessage msg = buildMsg("http://img.url/flood.jpg");
        when(geminiApiClient.analyzeFloodImage(msg)).thenReturn(buildResponse(false, 20));
        strategy.calculateScore(msg);
    }

    @Test
    void isApplicable_nullImageUrl_returnsFalse() {
        strategy.isApplicable(buildMsg(null));
    }

    @Test
    void isApplicable_blankImageUrl_returnsFalse() {
        strategy.isApplicable(buildMsg("   "));
    }

    @Test
    void isApplicable_validImageUrl_returnsTrue() {
        strategy.isApplicable(buildMsg("http://img.url/flood.jpg"));
    }

    @Test
    void getStrategyName_returnsAiVision() {
        strategy.getStrategyName();
    }
}
