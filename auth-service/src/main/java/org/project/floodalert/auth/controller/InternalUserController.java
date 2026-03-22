package org.project.floodalert.auth.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.project.floodalert.auth.service.ReputationCacheService;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalUserController {

    ReputationCacheService reputationCacheService;


    @GetMapping("/{userId}/reputation")
    public ApiResponse<Integer> getUserReputation(@PathVariable UUID userId) {
        return ApiResponse.success(reputationCacheService.getOrLoad(userId));
    }
}
