package org.project.floodalert.notification.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloodLifecycleEvent {

    String eventId;
    String type;
    Double waterLevel;
    String severityLevel;
    String location;
    Double lat;
    Double lon;
    String source;

    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();
}
