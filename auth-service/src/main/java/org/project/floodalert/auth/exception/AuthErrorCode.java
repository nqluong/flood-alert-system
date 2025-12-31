package org.project.floodalert.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
    ACCOUNT_DISABLED(2002, "Tài khoản đã bị vô hiệu hóa", HttpStatus.FORBIDDEN),
    INVALID_OTP(2101, "Mã OTP không hợp lệ", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(2102, "Mã OTP đã hết hạn", HttpStatus.BAD_REQUEST),
    TOO_MANY_LOGIN_ATTEMPTS(2103, "Quá nhiều lần đăng nhập sai", HttpStatus.TOO_MANY_REQUESTS),
    PASSWORD_TOO_WEAK(2104, "Mật khẩu quá yếu", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(2105, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(2106 ,"Email đã tồn tại", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS(2107, "Số điện thoại đã tồn tại", HttpStatus.CONFLICT),
    ADDRESS_NOT_FOUND(2108, "Không tìm thấy địa chỉ", HttpStatus.NOT_FOUND),

    ROLE_NOT_FOUND(3001, "Không tìm thấy role", HttpStatus.NOT_FOUND),
    ROLE_ALREADY_EXISTS(3002, "Role đã tồn tại", HttpStatus.CONFLICT),
    ROLE_IN_USE(3003, "Role đang được sử dụng, không thể xóa", HttpStatus.CONFLICT),
    USER_ROLE_NOT_FOUND(3004, "Không tìm thấy phân quyền người dùng", HttpStatus.NOT_FOUND),
    USER_ROLE_ALREADY_EXISTS(3005, "Người dùng đã có role này", HttpStatus.CONFLICT),
    INVALID_ROLE_DATA(3006, "Dữ liệu role không hợp lệ", HttpStatus.BAD_REQUEST),

    WRONG_AUTH_PROVIDER(1006, "Sai nhà cung cấp xác thực", HttpStatus.BAD_REQUEST),
    FIREBASE_AUTH_ERROR(1100, "Xác thực với firebase bị lỗi", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1101, "Token từ firebase đã bị hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED(1102, "Token Firebase đã bị thu hồi", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(1103, "Token Firebase không hợp lệ", HttpStatus.UNAUTHORIZED),
    FIREBASE_USER_NOT_FOUND(1104, "Không tìm thấy người dùng trên Firebase", HttpStatus.NOT_FOUND),
    FIREBASE_CONFIG_ERROR(1105, "Lỗi cấu hình Firebase", HttpStatus.INTERNAL_SERVER_ERROR),



    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
