package org.project.floodalert.floodprocessor.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FloodStatus {
    SAFE("An toàn", 0),
    WARNING("Cảnh báo", 1),
    DANGER("Nguy hiểm", 2),
    UNKNOWN("Không xác định", -1);

    private final String description;
    private final int severity;
}
