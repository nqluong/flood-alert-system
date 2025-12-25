package org.project.floodcore.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.dto.response.ErrorResponse;
import org.project.floodcore.apigateway.dto.response.VerifyTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${app.services.auth.url}")
    private String AUTH_SERVICE_URL;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Bỏ qua xác thực cho public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return unauthorized(exchange.getResponse(), "Thiếu token xác thực");
        }
        return verifyTokenWithAuthService(token)
                .flatMap(response -> {
                    if (response.isValid()) {
                        ServerHttpRequest mutatedRequest = exchange.getRequest()
                                .mutate()
                                .header("X-User-Id", response.getUserId().toString())
                                .header("X-User-Email", response.getEmail())
                                .header("X-User-Roles", String.join(",", response.getRoles()))
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    } else {
                        return unauthorized(exchange.getResponse(),
                                response.getInvalidReason() != null ?
                                        response.getInvalidReason() : "Token không hợp lệ");
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error verifying token: {}", error.getMessage());
                    return unauthorized(exchange.getResponse(), "Lỗi xác thực token");
                });
    }

    private Mono<VerifyTokenResponse> verifyTokenWithAuthService(String token) {
        return webClientBuilder.build()
                .get()
                .uri(AUTH_SERVICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .map(apiResponse -> {
                    try {
                        return objectMapper.convertValue(
                                apiResponse.getData(),
                                VerifyTokenResponse.class
                        );
                    } catch (Exception e) {
                        log.error("Error converting response", e);
                        return VerifyTokenResponse.invalid("Lỗi xử lý response");
                    }
                })
                .timeout(Duration.ofSeconds(5)) // Timeout after 5 seconds
                .onErrorReturn(VerifyTokenResponse.invalid("Không thể kết nối auth service"));
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse error = ErrorResponse.builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message(message)
                .build();

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(error);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
    private String extractToken(ServerHttpRequest request) {
        List<String> headers = request.getHeaders().get("Authorization");
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String authHeader = headers.get(0);
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

}
