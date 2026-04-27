package org.project.floodalert.floodcore.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.security.SecurityContextUtils;
import org.project.floodalert.floodcore.dto.response.FileUploadResponse;
import org.project.floodalert.floodcore.service.FirebaseStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class FileUploadController {

    private final FirebaseStorageService firebaseStorageService;

    @GetMapping("/upload-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FileUploadResponse>> getUploadUrl(
            @RequestParam("extension") @NotBlank(message = "Extension không được để trống") String extension) {
        
        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        log.info("GET /api/v1/reports/upload-url - userId={}, extension={}", userId, extension);

        FileUploadResponse response = firebaseStorageService.generateUploadUrl(userId, extension);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
