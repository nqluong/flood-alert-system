package org.project.floodalert.floodcore.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CoreErrorCode implements BaseErrorCode {
    SENSOR_ALREADY_EXISTS(4001, "Cảm biến đã tồn tại trong hệ thống", HttpStatus.CONFLICT),
    SENSOR_NOT_FOUND(4002, "Không tìm thấy cảm biến", HttpStatus.NOT_FOUND),
    INVALID_THRESHOLD(4003, "Ngưỡng nguy hiểm phải lớn hơn ngưỡng cảnh báo", HttpStatus.BAD_REQUEST),
    INVALID_COORDINATES(4004, "Tọa độ không hợp lệ", HttpStatus.BAD_REQUEST),
    SENSOR_ALREADY_DELETED(4005, "Cảm biến đã bị xóa trước đó", HttpStatus.CONFLICT),
    SENSOR_NOT_DELETED(4006, "Cảm biến không ở trạng thái deleted", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_TRANSITION(4007, "Chuyển đổi trạng thái không hợp lệ", HttpStatus.BAD_REQUEST),
    SENSOR_STATUS_SAME(4008, "Trạng thái sensor không thay đổi", HttpStatus.CONFLICT),
    INVALID_SENSOR_STATUS(4009, "Trạng thái sensor không hợp lệ", HttpStatus.BAD_REQUEST),

    USER_REPORT_NOT_FOUND(4010, "Không tìm thấy báo cáo", HttpStatus.NOT_FOUND),
    USER_REPORT_ALREADY_REVIEWED(4011, "Báo cáo đã được xét duyệt trước đó", HttpStatus.CONFLICT),

    FIREBASE_STORAGE_ERROR(5003, "Lỗi khi tạo URL upload", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_EXTENSION(4012, "Định dạng file không hợp lệ", HttpStatus.BAD_REQUEST),

    REDIS_SYNC_FAILED(5001, "Đồng bộ dữ liệu Redis thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR(5004, "Lỗi khi gọi dịch vụ bên ngoài", HttpStatus.SERVICE_UNAVAILABLE),

    DATABASE_ERROR(5002, "Lỗi cơ sở dữ liệu", HttpStatus.INTERNAL_SERVER_ERROR),

    INTERNAL_SERVER_ERROR(5000, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(4000, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST);
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
