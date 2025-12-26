package org.project.floodalert.auth.service.impl;

import jakarta.security.auth.message.AuthException;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.dto.request.AssignRoleRequest;
import org.project.floodalert.auth.dto.response.UserRoleResponse;
import org.project.floodalert.auth.exception.AuthErrorCode;
import org.project.floodalert.auth.mapper.UserRoleMapper;
import org.project.floodalert.auth.model.Role;
import org.project.floodalert.auth.model.User;
import org.project.floodalert.auth.model.UserRole;
import org.project.floodalert.auth.repository.RoleRepository;
import org.project.floodalert.auth.repository.UserRepository;
import org.project.floodalert.auth.repository.UserRoleRepository;
import org.project.floodalert.auth.service.UserRoleService;
import org.project.floodalert.common.exception.AppException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRoleServiceImpl implements UserRoleService {

    UserRoleRepository userRoleRepository;
    RoleRepository roleRepository;
    UserRepository userRepository;
    UserRoleMapper userRoleMapper;
    UserRoleValidator userRoleValidator;

    @Override
    @Transactional
    public UserRoleResponse assignRole(AssignRoleRequest request) {
        UUID userId = UUID.fromString(request.getUserId());
        UUID roleId = UUID.fromString(request.getRoleId());

        userRoleValidator.validateAssignRequest(userId, roleId);

        // Kiểm tra user và role tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(AuthErrorCode.ROLE_NOT_FOUND));

        // Kiểm tra user đã có role này chưa
        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new AppException(AuthErrorCode.USER_ROLE_ALREADY_EXISTS);
        }

        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .assignedAt(LocalDateTime.now())
                .build();

        UserRole savedUserRole = userRoleRepository.save(userRole);

        return userRoleMapper.toResponse(savedUserRole, user, role);
    }

    @Override
    @Transactional
    public void removeRole(UUID userId, UUID roleId) {
        if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new AppException(AuthErrorCode.USER_ROLE_NOT_FOUND);
        }

        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        log.info("Role removed successfully");
    }

    @Override
    public List<UserRoleResponse> getUserRoles(UUID userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);

        return userRoles.stream()
                .map(ur -> {
                    User user = userRepository.findById(ur.getUserId()).orElse(null);
                    Role role = roleRepository.findById(ur.getRoleId()).orElse(null);
                    return userRoleMapper.toResponse(ur, user, role);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRoleResponse> getRoleUsers(UUID roleId) {
        List<UserRole> userRoles = userRoleRepository.findByRoleId(roleId);

        return userRoles.stream()
                .map(ur -> {
                    User user = userRepository.findById(ur.getUserId()).orElse(null);
                    Role role = roleRepository.findById(ur.getRoleId()).orElse(null);
                    return userRoleMapper.toResponse(ur, user, role);
                })
                .collect(Collectors.toList());
    }
}
