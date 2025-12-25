package org.project.floodalert.auth.service.impl;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.constants.RoleConstants;
import org.project.floodalert.auth.model.Role;
import org.project.floodalert.auth.model.UserRole;
import org.project.floodalert.auth.repository.RoleRepository;
import org.project.floodalert.auth.repository.UserRoleRepository;
import org.project.floodalert.auth.service.RoleService;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleServiceImpl implements RoleService {

    RoleRepository roleRepository;
    UserRoleRepository userRoleRepository;

    @Override
    @Transactional
    public void assignRoleToUser(UUID userId, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new AppException(
                        ErrorCode.ROLE_NOT_FOUND,
                        "Role không tồn tại: " + roleName
                ));

        // Kiểm tra user đã có role này chưa
        if (userRoleRepository.hasRole(userId, roleName)) {
            log.info("User {} already has role {}", userId, roleName);
            return;
        }

        // Gán role
        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(role.getId())
                .build();

        userRoleRepository.save(userRole);
        log.info("Assigned role {} to user {}", roleName, userId);
    }

    @Override
    @Transactional
    public void assignRoleDefaultToUser(UUID userId) {
        assignRoleToUser(userId, RoleConstants.USER);
    }

    @Override
    @Transactional
    public void assignRolesToUser(UUID userId, String[] roleNames) {
        for (String roleName : roleNames) {
            assignRoleToUser(userId, roleName);
        }
    }
}
