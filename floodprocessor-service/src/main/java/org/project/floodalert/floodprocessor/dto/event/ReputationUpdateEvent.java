package org.project.floodalert.floodprocessor.dto.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.project.floodalert.floodprocessor.enums.ReputationReason;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReputationUpdateEvent {

    /** User ID của người nhận reward/penalty */
    UUID userId;

    /** Flood Event ID liên quan */
    String eventId;

    /** Lý do reward/penalty */
    ReputationReason reason;

    /** Điểm thay đổi (dương = reward, âm = penalty) */
    Integer points;

    /** Report ID gốc gây ra sự thay đổi này */
    String reportId;

    Double lat;

    Double lon;

    String address;

    /** Timestamp khi event được tạo */
    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();
}
