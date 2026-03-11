package org.project.floodalert.floodprocessor.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

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
}
