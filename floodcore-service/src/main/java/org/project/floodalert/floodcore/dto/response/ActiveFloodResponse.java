package org.project.floodalert.floodcore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActiveFloodResponse {

    String eventId;
    Double lat;
    Double lon;
    String severityLevel;
    Double waterLevel;
    String location;
    String updatedAt;
}

