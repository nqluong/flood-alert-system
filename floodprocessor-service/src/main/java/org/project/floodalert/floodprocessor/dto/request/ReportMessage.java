package org.project.floodalert.floodprocessor.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportMessage {

    /** Mã định danh duy nhất của báo cáo */
    String reportId;

    UUID userId;

    Double lat;

    Double lon;
    String severityLevel;
    String imageUrl;

    String description;

    @Builder.Default
    Instant receivedAt = Instant.now();
}
