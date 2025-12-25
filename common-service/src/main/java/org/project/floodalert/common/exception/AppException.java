package org.project.floodalert.common.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException{
    private final BaseErrorCode  errorCode;
    private final String details;

    public AppException(BaseErrorCode  errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public AppException(BaseErrorCode  errorCode, String details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public AppException(BaseErrorCode  errorCode, String details, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = details;
    }
}
