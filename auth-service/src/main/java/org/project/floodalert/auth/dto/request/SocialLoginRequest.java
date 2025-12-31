package org.project.floodalert.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SocialLoginRequest {

    @NotBlank(message = "ID Token không được để trống")
    String idToken;

    String provider;
}
