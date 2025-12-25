package org.project.floodalert.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseErrorCode {
    VALIDATION_ERROR(1001, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1002, "Tài nguyên không tồn tại", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR(1003, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    ACCESS_DENIED(1004, "Truy cập bị từ chối", HttpStatus.FORBIDDEN),

    INVALID_CREDENTIALS(2001, "Email hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(2002, "Tài khoản đã bị vô hiệu hóa", HttpStatus.FORBIDDEN),
    ACCOUNT_BANNED(2003, "Tài khoản đã bị cấm", HttpStatus.FORBIDDEN),
    WRONG_AUTH_PROVIDER(2004, "Phương thức đăng nhập không đúng", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(2005, "Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(2006, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED(2007, "Token đã bị thu hồi", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS(2008, "Email đã tồn tại", HttpStatus.CONFLICT),

    USER_NOT_FOUND(3001, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    INVALID_USER_STATUS(3002, "Trạng thái người dùng không hợp lệ", HttpStatus.BAD_REQUEST),

    ROLE_NOT_FOUND(4001, "Vai trò không tồn tại", HttpStatus.NOT_FOUND),
    INSUFFICIENT_PERMISSIONS(4002, "Không đủ quyền", HttpStatus.FORBIDDEN);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
