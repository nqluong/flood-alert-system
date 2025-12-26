package org.project.floodalert.auth.service.impl;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.constants.RoleConstants;
import org.project.floodalert.auth.dto.request.CreateRoleRequest;
import org.project.floodalert.auth.dto.request.UpdateRoleRequest;
import org.project.floodalert.auth.dto.response.RoleResponse;
import org.project.floodalert.auth.exception.AuthErrorCode;
import org.project.floodalert.auth.mapper.RoleMapper;
import org.project.floodalert.auth.model.Role;
import org.project.floodalert.auth.model.UserRole;
import org.project.floodalert.auth.repository.RoleRepository;
import org.project.floodalert.auth.repository.UserRoleRepository;
import org.project.floodalert.auth.service.RoleService;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleServiceImpl implements RoleService {

    RoleRepository roleRepository;
    UserRoleRepository userRoleRepository;
    RoleValidator roleValidator;
    RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        roleValidator.validateCreateRequest(request);

        if (roleRepository.existsByName(request.getName())) {
            throw new AppException(AuthErrorCode.ROLE_ALREADY_EXISTS);
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        Role savedRole = roleRepository.save(role);
        log.info("Role created successfully: {}", savedRole.getId());

        return roleMapper.toResponse(savedRole);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(UUID roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(AuthErrorCode.ROLE_NOT_FOUND));

        roleValidator.validateUpdateRequest(request, role);

        if (request.getName() != null && !request.getName().equals(role.getName())) {
            if (roleRepository.existsByName(request.getName())) {
                throw new AppException(AuthErrorCode.ROLE_ALREADY_EXISTS);
            }
            role.setName(request.getName());
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        Role updatedRole = roleRepository.save(role);
        log.info("Role updated successfully: {}", roleId);

        return roleMapper.toResponse(updatedRole);
    }

    @Override
    @Transactional
    public void deleteRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(AuthErrorCode.ROLE_NOT_FOUND));

        // Kiểm tra role có đang được sử dụng không
        List<UserRole> userRoles = userRoleRepository.findByRoleId(roleId);
        if (!userRoles.isEmpty()) {
            throw new AppException(AuthErrorCode.ROLE_IN_USE);
        }

        roleRepository.delete(role);
    }

    @Override
    public RoleResponse getRoleById(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(AuthErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse getRoleByName(String name) {
        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> new AppException(AuthErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toResponse(role);
    }

    @Override
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toResponse)
                .collect(Collectors.toList());
    }

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
