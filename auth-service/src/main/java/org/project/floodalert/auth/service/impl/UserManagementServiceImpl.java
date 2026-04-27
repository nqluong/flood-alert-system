package org.project.floodalert.auth.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.dto.response.UserListResponse;
import org.project.floodalert.auth.model.User;
import org.project.floodalert.auth.model.UserProfile;
import org.project.floodalert.auth.repository.UserProfileRepository;
import org.project.floodalert.auth.repository.UserRepository;
import org.project.floodalert.auth.repository.UserRoleRepository;
import org.project.floodalert.auth.service.UserManagementService;
import org.project.floodalert.common.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserManagementServiceImpl implements UserManagementService {

    UserRepository userRepository;
    UserProfileRepository userProfileRepository;
    UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserListResponse> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        
        if (userPage.isEmpty()) {
            return PageResponse.of(
                List.of(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                0L,
                0
            );
        }

        List<UUID> userIds = userPage.getContent().stream()
                .map(User::getId)
                .toList();

        Map<UUID, UserProfile> profileMap = userProfileRepository.findAll().stream()
                .filter(profile -> userIds.contains(profile.getUserId()))
                .collect(Collectors.toMap(UserProfile::getUserId, profile -> profile));

        Map<UUID, List<String>> rolesMap = userIds.stream()
                .collect(Collectors.toMap(
                        userId -> userId,
                        userRoleRepository::findRoleNamesByUserId
                ));

        List<UserListResponse> userListResponses = userPage.getContent().stream()
                .map(user -> mapToUserListResponse(user, profileMap.get(user.getId()), rolesMap.get(user.getId())))
                .toList();

        return PageResponse.of(
                userListResponses,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    private UserListResponse mapToUserListResponse(User user, UserProfile profile, List<String> roles) {
        return UserListResponse.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(profile != null ? profile.getFullName() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .status(user.getStatus().toString())
                .emailVerified(user.getEmailVerified())
                .authProvider(user.getAuthProvider().toString())
                .reputationScore(profile != null ? profile.getReputationScore() : 0)
                .totalReportsSubmitted(profile != null ? profile.getTotalReportsSubmitted() : 0)
                .roles(roles != null ? roles : List.of())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
