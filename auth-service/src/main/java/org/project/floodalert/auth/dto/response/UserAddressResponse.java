package org.project.floodalert.auth.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAddressResponse {
    String id;
    String addressText;
    Double lat;
    Double lon;
    Boolean isPrimary;
    String addressType;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
