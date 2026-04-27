package org.project.floodalert.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FcmTokenRequest {
    @NotBlank(message = "FCM Token không được để trống")
    String token;

    @NotBlank(message = "Loại thiết bị không được để trống")
    String deviceType;

    @NotBlank(message = "ID thiết bị không được để trống")
    String deviceId;
    String deviceName;
}
