package org.project.floodalert.auth.service.impl;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.dto.request.UserProfileUpdateRequest;
import org.project.floodalert.auth.dto.response.UserProfileResponse;
import org.project.floodalert.auth.exception.AuthErrorCode;
import org.project.floodalert.auth.model.User;
import org.project.floodalert.auth.model.UserProfile;
import org.project.floodalert.auth.repository.UserProfileRepository;
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
    UserProfileRepository userProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = findUserById(userId);
        UserProfile profile = findProfileOrEmpty(userId);
        return mapToProfileResponse(user, profile);
    }

    @Override
    public UserProfileResponse updateProfile(UUID userId, UserProfileUpdateRequest request) {
        User user = findUserById(userId);
        UserProfile profile = findOrCreateProfile(userId);

        profile.setFullName(request.getFullName());
        profile.setAvatarUrl(request.getAvatarUrl());
        user.setPhone(request.getPhoneNumber());

        User updatedUser = userRepository.save(user);
        UserProfile updatedProfile = userProfileRepository.save(profile);
        return mapToProfileResponse(updatedUser, updatedProfile);

    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với id: " + userId));
    }

    private UserProfile findOrCreateProfile(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseGet(() -> userProfileRepository.save(UserProfile.builder()
                        .userId(userId)
                        .reputationScore(50)
                        .totalReportsSubmitted(0)
                        .build()));
    }

    private UserProfile findProfileOrEmpty(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .orElse(UserProfile.builder().userId(userId).build());
    }

    private UserProfileResponse mapToProfileResponse(User user, UserProfile profile) {
        return UserProfileResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(profile.getFullName())
                .phone(user.getPhone())
                .avatarUrl(profile.getAvatarUrl())
                .status(user.getStatus().toString())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
