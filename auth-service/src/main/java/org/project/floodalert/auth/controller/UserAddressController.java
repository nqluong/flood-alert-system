package org.project.floodalert.auth.controller;

import lombok.RequiredArgsConstructor;
import org.project.floodalert.auth.dto.request.UserAddressRequest;
import org.project.floodalert.auth.dto.response.UserAddressResponse;
import org.project.floodalert.auth.service.UserAddressService;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.security.InternalUserDetails;
import org.project.floodalert.common.security.annotation.RequireOwnershipOrAdmin;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user/addresses")
@RequiredArgsConstructor
public class UserAddressController {
    private final UserAddressService userAddressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAddressResponse>>> getUserAddresses(
            Authentication authentication) {
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());
        List<UserAddressResponse> responses = userAddressService.getUserAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/user/{userId}")
    @RequireOwnershipOrAdmin(userIdParam = "userId")
    public ResponseEntity<ApiResponse<List<UserAddressResponse>>> getUserAddressesByUserId(
            @PathVariable("userId") UUID userId) {
        List<UserAddressResponse> responses = userAddressService.getUserAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserAddressResponse>> createAddressForUser(
            @PathVariable("userId") UUID userId,
            @RequestBody UserAddressRequest addressRequest) {
        UserAddressResponse response = userAddressService.createAddress(userId, addressRequest);
        return ResponseEntity.ok(ApiResponse.success("Địa chỉ đã được thêm thành công", response));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<UserAddressResponse>> getUserAddressById(
            @PathVariable("addressId") UUID addressId,
            Authentication authentication) {
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());
        UserAddressResponse response = userAddressService.getUserAddressById(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserAddressResponse>> createAddress(
            @RequestBody UserAddressRequest addressRequest,
            Authentication authentication) {
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());
        UserAddressResponse response = userAddressService.createAddress(userId, addressRequest);
        return ResponseEntity.ok(ApiResponse.success("Địa chỉ đã được thêm thành công", response));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<UserAddressResponse>> updateAddress(
            @PathVariable("addressId") UUID addressId,
            @RequestBody UserAddressRequest addressRequest,
            Authentication authentication) {
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());
        UserAddressResponse response = userAddressService.updateAddress(userId, addressId, addressRequest);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ thành công", response));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable("addressId") UUID addressId,
            Authentication authentication) {
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());
        userAddressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Xóa địa chỉ thành công", null));
    }

    @PatchMapping("/{addressId}/set-primary")
    public ResponseEntity<ApiResponse<UserAddressResponse>> setPrimaryAddress(
            @PathVariable("addressId") UUID addressId,
            Authentication authentication) {
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());
        UserAddressResponse response = userAddressService.setPrimaryAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Đặt địa chỉ chính thành công", response));
    }

    @DeleteMapping("/admin/{addressId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminDeleteAddress(
            @PathVariable("addressId") UUID addressId) {
        userAddressService.adminDeleteAddress(addressId);
        return ResponseEntity.ok(ApiResponse.success("Xóa địa chỉ thành công", null));
    }
}
