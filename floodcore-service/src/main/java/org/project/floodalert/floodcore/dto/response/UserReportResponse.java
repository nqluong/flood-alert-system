package org.project.floodalert.floodcore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserReportResponse {

    UUID id;

    String reportId;
    UUID userId;
    String description;
    String imageUrls;

    String severityLevel;
    Double lat;
    Double lon;
    String status;
    LocalDateTime createdAt;

    String message;
}
