package org.project.floodalert.floodcore.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserReportEvent {

    String reportId;

    UUID userId;
    String imageUrl;
    String severityLevel;
    Double lat;
    Double lon;

    String description;

    @Builder.Default
    LocalDateTime reportedAt = LocalDateTime.now();
}

