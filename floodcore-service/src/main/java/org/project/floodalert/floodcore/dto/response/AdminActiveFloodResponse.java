package org.project.floodalert.floodcore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminActiveFloodResponse {

    String eventId;
    Double lat;
    Double lon;
    String location;

    Double waterLevel;

    String severityLevel;

    String status;

    String source;

    LocalDateTime updatedAt;
}
