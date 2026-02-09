package org.project.floodalert.floodprocessor.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProcessorErrorCode implements BaseErrorCode {

    PROCESSING_FAILED(1001, "Processing of the flood data failed", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_DATA_FORMAT(1002, "The flood data format is invalid", HttpStatus.BAD_REQUEST),
    REDIS_CONNECTION_FAILED(1003, "Redis connection failed", HttpStatus.SERVICE_UNAVAILABLE),
    REDIS_OPERATION_FAILED(1004, "Redis operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    ENRICHED_DATA_NULL(1005, "Dữ liệu sau khi enriched bị rỗng", HttpStatus.BAD_REQUEST),
    SENSOR_ID_MISSING(1006, "Sensor ID bị thiếu trong dữ liệu", HttpStatus.BAD_REQUEST),
    WATER_LEVEL_INVALID(1007, "Mức nước không hợp lệ", HttpStatus.BAD_REQUEST),
    THRESHOLD_MISSING(1008, "Ngưỡng cảnh báo/nguy hiểm bị thiếu", HttpStatus.BAD_REQUEST),

    INTERNAL_SERVER_ERROR(5000, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(4000, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

}
