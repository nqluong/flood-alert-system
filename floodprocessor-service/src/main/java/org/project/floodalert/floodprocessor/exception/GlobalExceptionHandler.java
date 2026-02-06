package org.project.floodalert.floodprocessor.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ErrorResponse;
import org.project.floodalert.common.dto.response.ValidationResponse;
import org.project.floodalert.common.exception.AppException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(
            AppException ex,
            HttpServletRequest request) {

        log.error("Lỗi nghiệp vụ: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .code(ex.getErrorCode().getCode())
                .message(ex.getMessage())
                .details(ex.getErrorCode().getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.error("Lỗi validation: {}", ex.getMessage());

        List<ValidationResponse> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error ->{
                    FieldError fieldError = (FieldError) error;
                    return ValidationResponse.builder()
                            .field(fieldError.getField())
                            .message(fieldError.getDefaultMessage())
                            .rejectedValue(fieldError.getRejectedValue())
                            .build();
                })
                .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .code(ProcessorErrorCode.VALIDATION_ERROR.getCode())
                .message(ProcessorErrorCode.VALIDATION_ERROR.getMessage())
                .details(errors.toString())
                .path(request.getRequestURI())
                .validationErrors(errors)
                .build();


        return ResponseEntity
                .status(ProcessorErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Lỗi hệ thống không xác định: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .code(ProcessorErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ex.getMessage())
                .details(ProcessorErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(ProcessorErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(errorResponse);
    }
}
