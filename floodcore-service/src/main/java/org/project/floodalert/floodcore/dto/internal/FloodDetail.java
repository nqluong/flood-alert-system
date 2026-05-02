package org.project.floodalert.floodcore.dto.internal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloodDetail {

    String eventId;
    Double lat;
    Double lon;
    Double waterLevel;  // Mực nước (cm) - dùng để xác định mức độ nguy hiểm
    String severityLevel;
}
