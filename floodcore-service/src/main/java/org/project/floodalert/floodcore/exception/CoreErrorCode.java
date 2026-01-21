package org.project.floodalert.floodcore.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CoreErrorCode implements BaseErrorCode {
    ;
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
