package org.project.floodalert.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRoleRequest {
    @Size(min = 3, max = 50, message = "Tên role phải từ 3-50 ký tự")
    String name;

    @Size(max = 255, message = "Mô tả không vượt quá 255 ký tự")
    String description;
}
