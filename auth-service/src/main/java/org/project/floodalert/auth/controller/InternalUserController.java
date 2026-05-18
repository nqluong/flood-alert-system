package org.project.floodalert.auth.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.service.ReputationCacheService;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.exception.AppException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalUserController {

    ReputationCacheService reputationCacheService;

    @GetMapping("/{userId}/reputation")
    public ResponseEntity<ApiResponse<Integer>> getUserReputation(@PathVariable(name = "userId") UUID userId) {
        try {
            int score = reputationCacheService.getOrLoad(userId);
            log.debug("[INTERNAL] Reputation query: userId={}, score={}", userId, score);
            return ResponseEntity.ok(ApiResponse.success(score));
        } catch (AppException e) {
            log.warn("[INTERNAL] userId={} not found, returning 404 for caller fallback", userId);
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(ApiResponse.<Integer>builder()
                            .success(false)
                            .code(e.getErrorCode().getCode())
                            .message(e.getErrorCode().getMessage())
                            .build());
        }
    }
}
