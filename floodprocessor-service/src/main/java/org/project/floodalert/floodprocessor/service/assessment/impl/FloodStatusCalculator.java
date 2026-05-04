package org.project.floodalert.floodprocessor.service.assessment.impl;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FloodStatusCalculator {

    public FloodStatus calculate(Double waterLevel, Double warningThreshold, Double dangerThreshold) {
        if (!isThresholdsValid(warningThreshold, dangerThreshold)) {
            return FloodStatus.UNKNOWN;
        }

        if (waterLevel == null) {
            log.warn("Thiếu thông tin mức nước");
            return FloodStatus.UNKNOWN;
        }

        return determineStatusByThreshold(waterLevel, warningThreshold, dangerThreshold);
    }

    private boolean isThresholdsValid(Double warningThreshold, Double dangerThreshold) {
        return warningThreshold != null && dangerThreshold != null;
    }

    private FloodStatus determineStatusByThreshold(
            Double waterLevel,
            Double warningThreshold,
            Double dangerThreshold) {

        if (waterLevel >= dangerThreshold) {
            return FloodStatus.DANGER;
        }

        // Kiểm tra ngưỡng cảnh báo
        if (waterLevel >= warningThreshold) {
            return FloodStatus.WARNING;
        }

        return FloodStatus.SAFE;
    }
}
