package org.project.floodalert.floodprocessor.service.assessment.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.enums.FloodStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FloodStatusCalculatorTest {

    @InjectMocks
    private FloodStatusCalculator calculator;

    // warningThreshold = null → UNKNOWN
    @Test
    void calculate_nullWarningThreshold_returnsUnknown() {
        assertEquals(FloodStatus.UNKNOWN, calculator.calculate(1.0, null, 5.0));
    }

    // dangerThreshold = null → UNKNOWN
    @Test
    void calculate_nullDangerThreshold_returnsUnknown() {
        assertEquals(FloodStatus.UNKNOWN, calculator.calculate(1.0, 3.0, null));
    }

    // waterLevel = null → UNKNOWN
    @Test
    void calculate_nullWaterLevel_returnsUnknown() {
        assertEquals(FloodStatus.UNKNOWN, calculator.calculate(null, 3.0, 5.0));
    }

    // waterLevel >= dangerThreshold → DANGER
    @Test
    void calculate_waterLevelAtDangerThreshold_returnsDanger() {
        assertEquals(FloodStatus.DANGER, calculator.calculate(5.0, 3.0, 5.0));
    }

    // waterLevel > dangerThreshold → DANGER
    @Test
    void calculate_waterLevelAboveDanger_returnsDanger() {
        assertEquals(FloodStatus.DANGER, calculator.calculate(7.0, 3.0, 5.0));
    }

    // warningThreshold <= waterLevel < dangerThreshold → WARNING
    @Test
    void calculate_waterLevelAtWarningThreshold_returnsWarning() {
        assertEquals(FloodStatus.WARNING, calculator.calculate(3.0, 3.0, 5.0));
    }

    // waterLevel < warningThreshold → SAFE
    @Test
    void calculate_waterLevelBelowWarning_returnsSafe() {
        assertEquals(FloodStatus.SAFE, calculator.calculate(1.0, 3.0, 5.0));
    }
}