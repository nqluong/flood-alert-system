package org.project.floodalert.notification.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationPreferenceResponse {

    Boolean enabled;
    Boolean floodAlerts;
    Boolean quietHoursEnabled;
    LocalTime quietHoursStart;
    LocalTime quietHoursEnd;
    Integer alertRadiusMeters;
    Boolean preferPush;
}
