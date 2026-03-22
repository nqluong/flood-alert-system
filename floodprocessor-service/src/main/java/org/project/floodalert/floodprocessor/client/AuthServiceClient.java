package org.project.floodalert.floodprocessor.client;

import org.project.floodalert.common.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;


@FeignClient(
    name = "auth-service",
    url = "${auth-service.url:http://localhost:8081}"
)
public interface AuthServiceClient {

    @GetMapping("/api/internal/users/{userId}/reputation")
    ApiResponse<Integer> getReputation(@PathVariable("userId") UUID userId);
}
