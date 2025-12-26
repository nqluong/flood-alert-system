package org.project.floodalert.auth.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRoleResponse {
    UUID id;
    UUID userId;
    String userEmail;
    String userFullName;
    UUID roleId;
    String roleName;
    LocalDateTime assignedAt;
}
