package org.project.floodalert.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.dto.request.AssignRoleRequest;
import org.project.floodalert.auth.dto.response.UserRoleResponse;
import org.project.floodalert.auth.service.UserRoleService;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/user-roles")
@RequiredArgsConstructor
public class UserRoleController {
    private final UserRoleService userRoleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserRoleResponse>> assignRole(
            @Valid @RequestBody AssignRoleRequest request) {
        UserRoleResponse response = userRoleService.assignRole(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gắn role cho người dùng thành công", response));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @RequestParam UUID userId,
            @RequestParam UUID roleId) {
        userRoleService.removeRole(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success("Xóa role thành công", null));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<UserRoleResponse>>> getUserRoles(
            @PathVariable UUID userId) {
        List<UserRoleResponse> responses = userRoleService.getUserRoles(userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserRoleResponse>>> getRoleUsers(
            @PathVariable UUID roleId) {
        List<UserRoleResponse> responses = userRoleService.getRoleUsers(roleId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
