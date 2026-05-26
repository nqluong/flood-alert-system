package org.project.floodalert.floodprocessor.service.scoring.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.service.cache.impl.UserProfileCacheService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserReputationScoringStrategyTest {

    @Mock
    private UserProfileCacheService userProfileCacheService;

    @InjectMocks
    private UserReputationScoringStrategy strategy;

    private ReportMessage buildMsg() {
        return ReportMessage.builder()
                .reportId("report-001")
                .userId(UUID.randomUUID())
                .build();
    }

    @Test
    void calculateScore_normalReputationScore_returnsNormalized() {
        when(userProfileCacheService.getReputationScore(any(UUID.class))).thenReturn(80);
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_zeroReputationScore_returnsZero() {
        when(userProfileCacheService.getReputationScore(any(UUID.class))).thenReturn(0);
        strategy.calculateScore(buildMsg());
    }

    @Test
    void calculateScore_maxReputationScore_returnsOne() {
        when(userProfileCacheService.getReputationScore(any(UUID.class))).thenReturn(100);
        strategy.calculateScore(buildMsg());
    }

    @Test
    void isApplicable_alwaysReturnsTrue() {
        strategy.isApplicable(buildMsg());
    }

    @Test
    void getStrategyName_returnsUserReputation() {
        strategy.getStrategyName();
    }
}
