package org.project.floodalert.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignRoleRequest {
    @NotBlank(message = "User ID không được để trống")
    String userId;

    @NotBlank(message = "Role ID không được để trống")
    String roleId;
}
