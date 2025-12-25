package org.project.floodalert.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
    INVALID_OTP(2101, "Mã OTP không hợp lệ", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(2102, "Mã OTP đã hết hạn", HttpStatus.BAD_REQUEST),
    TOO_MANY_LOGIN_ATTEMPTS(2103, "Quá nhiều lần đăng nhập sai", HttpStatus.TOO_MANY_REQUESTS),
    PASSWORD_TOO_WEAK(2104, "Mật khẩu quá yếu", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(2105, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(2106 ,"Email đã tồn tại", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS(2107, "Số điện thoại đã tồn tại", HttpStatus.CONFLICT),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
