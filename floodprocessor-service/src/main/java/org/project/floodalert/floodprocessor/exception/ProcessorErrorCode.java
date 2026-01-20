package org.project.floodalert.floodprocessor.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProcessorErrorCode implements BaseErrorCode {

    PROCESSING_FAILED(1001, "Processing of the flood data failed", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_DATA_FORMAT(1002, "The flood data format is invalid", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

}
