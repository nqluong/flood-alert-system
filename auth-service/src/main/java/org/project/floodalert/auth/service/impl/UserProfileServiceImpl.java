package org.project.floodalert.auth.service.impl;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.dto.request.UserProfileUpdateRequest;
import org.project.floodalert.auth.dto.response.UserProfileResponse;
import org.project.floodalert.auth.exception.AuthErrorCode;
import org.project.floodalert.auth.model.User;
import org.project.floodalert.auth.repository.UserRepository;
import org.project.floodalert.auth.service.UserProfileService;
import org.project.floodalert.common.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileServiceImpl implements UserProfileService {

    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = findUserById(userId);
        return mapToProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateProfile(UUID userId, UserProfileUpdateRequest request) {
        User user = findUserById(userId);

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhoneNumber());
        user.setAvatarUrl(request.getAvatarUrl());

        User updatedUser = userRepository.save(user);
        return mapToProfileResponse(updatedUser);

    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với id: " + userId));
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().toString())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
