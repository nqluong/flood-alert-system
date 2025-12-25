package org.project.floodalert.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ErrorResponse;
import org.project.floodalert.common.dto.response.ValidationResponse;
import org.project.floodalert.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e, HttpServletRequest request) {
        log.error("AppException: code={}, message={}, details={}",
                e.getErrorCode().getCode(), e.getMessage(), e.getDetails());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .code(e.getErrorCode().getCode())
                .message(e.getMessage())
                .details(e.getDetails())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ValidationResponse> validationErrors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    FieldError fieldError = (FieldError) error;
                    return ValidationResponse.builder()
                            .field(fieldError.getField())
                            .message(fieldError.getDefaultMessage())
                            .rejectedValue(fieldError.getRejectedValue())
                            .build();
                })
                .collect(Collectors.toList());

        log.error("Validation error: {}", validationErrors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .code(1001) // VALIDATION_ERROR code
                .message("Dữ liệu không hợp lệ")
                .details("Vui lòng kiểm tra lại thông tin đầu vào")
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.error("Authentication error: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .code(2005) // INVALID_TOKEN
                .message("Unauthorized")
                .details("Xác thực thất bại. Vui lòng đăng nhập lại.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.error("Access denied: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .code(1004) // ACCESS_DENIED code
                .message("Truy cập bị từ chối")
                .details("Bạn không có quyền truy cập tài nguyên này")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(errorResponse);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .code(1003) // INTERNAL_SERVER_ERROR code
                .message("Lỗi hệ thống")
                .details("Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
