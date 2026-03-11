package org.project.floodalert.floodcore.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.floodalert.floodcore.enums.FloodLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserReportRequest {
    String imageUrl;
    String description;
    Double lat;
    Double lon;
    FloodLevel level;
}
