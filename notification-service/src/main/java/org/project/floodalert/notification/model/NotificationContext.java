package org.project.floodalert.notification.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationContext {
    UUID userId;

    @Builder.Default
    boolean isNearActive = false;

    Double activeDistance;

    Double activeLat;
    Double activeLon;

    @Builder.Default
    List<String> affectedZones = new ArrayList<>();

    /** Distance (meters) từ mỗi static zone tới điểm ngập — dùng để trim zone ngoài radius */
    @Builder.Default
    Map<String, Double> zoneDistances = new HashMap<>();

    Double staticDistance;

    public boolean isAffected() {
        return isNearActive || !affectedZones.isEmpty();
    }

    public boolean isBothAffected() {
        return isNearActive && !affectedZones.isEmpty();
    }
}
