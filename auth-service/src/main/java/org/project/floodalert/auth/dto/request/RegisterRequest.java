package org.project.floodalert.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 số"
    )
    String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 255, message = "Họ và tên phải từ 2-255 ký tự")
    String fullName;

    @Pattern(
            regexp = "^(\\+84|0)[0-9]{9,10}$",
            message = "Số điện thoại không đúng định dạng"
    )
    String phoneNumber;
}
