package org.project.floodcore.apigateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ConnectTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodcore.apigateway.dto.response.ErrorResponse;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.lang.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

@Slf4j
@Order(-2)
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        String path = exchange.getRequest().getPath().value();

        // Nếu response đã committed (WebSocket handshake), không thể modify response
        if (response.isCommitted()) {
            log.warn("Response already committed for path [{}], cannot handle exception: {}", 
                    path, ex.getMessage());
            return Mono.empty();
        }

        HttpStatus status;
        String message;

        if (isConnectionRefused(ex)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Hệ thống hiện không khả dụng, vui lòng thử lại sau";
            log.error("Service unavailable – connection refused for path [{}]: {}", path, ex.getMessage());
        } else if (isConnectionTimeout(ex)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Hệ thống không phản hồi, yêu cầu đã hết thời gian chờ";
            log.error("Gateway timeout for path [{}]: {}", path, ex.getMessage());
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : rse.getMessage();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Đã xảy ra lỗi nội bộ tại cổng API";
            log.error("Unexpected gateway error for path [{}]", path, ex);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(status.value())
                .message(message)
                .path(path)
                .build();

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ErrorResponse", e);
            return response.setComplete();
        }
    }

    private boolean isConnectionRefused(Throwable ex) {
        if (ex instanceof ConnectException) return true;
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof ConnectException) return true;
            cause = cause.getCause();
        }
        return ex.getMessage() != null && ex.getMessage().contains("Connection refused");
    }

    private boolean isConnectionTimeout(Throwable ex) {
        if (ex instanceof ConnectTimeoutException) return true;
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof ConnectTimeoutException) return true;
            cause = cause.getCause();
        }
        return false;
    }
}
