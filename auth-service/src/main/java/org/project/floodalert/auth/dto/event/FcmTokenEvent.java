package org.project.floodalert.auth.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FcmTokenEvent {
    UUID userId;
    String token;
    Boolean isActive;
    String eventType; // UPSERT, DEACTIVATE, DEACTIVATE_ALL
    LocalDateTime timestamp;
}
