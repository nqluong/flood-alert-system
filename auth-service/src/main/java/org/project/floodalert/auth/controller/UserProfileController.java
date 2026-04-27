package org.project.floodalert.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.auth.dto.request.UserProfileUpdateRequest;
import org.project.floodalert.auth.dto.response.UserProfileResponse;
import org.project.floodalert.auth.service.UserProfileService;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.security.InternalUserDetails;
import org.project.floodalert.common.security.annotation.RequireOwnershipOrAdmin;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;


    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            Authentication authentication
    ) {
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());
        UserProfileResponse response = userProfileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cá nhân thành công", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request
            ) {
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());
        UserProfileResponse response = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin cá nhân thành công", response));
    }

    @GetMapping("/{userId}")
    @RequireOwnershipOrAdmin(userIdParam = "userId")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfileById(
            @PathVariable("userId") UUID userId
    ) {
        UserProfileResponse response = userProfileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cá nhân thành công", response));
    }

    @PutMapping("/{userId}")
    @RequireOwnershipOrAdmin(userIdParam = "userId")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserProfileById(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        UserProfileResponse response = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin cá nhân thành công", response));
    }

}
