package org.project.floodalert.floodcore.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.floodalert.floodcore.enums.LifecycleEventType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloodLifecycleEvent {

    String eventId;
    LifecycleEventType type;
    Double waterLevel;
    String severityLevel;
    String location;
    Double lat;
    Double lon;
    String source;
    String status;
    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();
}
