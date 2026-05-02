package org.project.floodalert.auth.controller;

import lombok.RequiredArgsConstructor;
import org.project.floodalert.auth.dto.request.UserSearchRequest;
import org.project.floodalert.auth.dto.response.UserListResponse;
import org.project.floodalert.auth.enums.UserStatus;
import org.project.floodalert.auth.service.UserManagementService;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.dto.response.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserListResponse>>> getAllUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "DESC") String sortDirection
    ) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PageResponse<UserListResponse> response = userManagementService.getAllUsers(pageable);
        
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách người dùng thành công", response)
        );
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserListResponse>>> searchUsers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) UserStatus status,
            @RequestParam(name = "roles", required = false) List<String> roles,
            @RequestParam(name = "emailVerified", required = false) Boolean emailVerified,
            @RequestParam(name = "authProvider", required = false) String authProvider,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "DESC") String sortDirection
    ) {
        UserSearchRequest searchRequest = UserSearchRequest.builder()
                .keyword(keyword)
                .status(status)
                .roles(roles)
                .emailVerified(emailVerified)
                .authProvider(authProvider)
                .build();

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PageResponse<UserListResponse> response = userManagementService.searchUsers(searchRequest, pageable);
        
        return ResponseEntity.ok(
                ApiResponse.success("Tìm kiếm người dùng thành công", response)
        );
    }
}
