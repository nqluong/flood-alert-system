package org.project.floodalert.notification.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationPreferenceDTO {

    @NotNull(message = "Enabled status is required")
    Boolean enabled;

    @NotNull(message = "Flood alerts status is required")
    Boolean floodAlerts;

    @NotNull(message = "Quiet hours enabled status is required")
    Boolean quietHoursEnabled;

    LocalTime quietHoursStart;

    LocalTime quietHoursEnd;

    @NotNull(message = "Alert radius is required")
    @Min(value = 100, message = "Alert radius must be at least 100 meters")
    Integer alertRadiusMeters;

    @NotNull(message = "Prefer push status is required")
    Boolean preferPush;
}
