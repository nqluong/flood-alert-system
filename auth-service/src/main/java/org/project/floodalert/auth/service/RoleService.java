package org.project.floodalert.auth.service;

import org.project.floodalert.auth.dto.request.CreateRoleRequest;
import org.project.floodalert.auth.dto.request.UpdateRoleRequest;
import org.project.floodalert.auth.dto.response.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    void assignRoleToUser(UUID userId, String roleName);

    void assignRoleDefaultToUser(UUID userId);

    void assignRolesToUser(UUID userId, String[] roleNames);

    RoleResponse createRole(CreateRoleRequest request);

    RoleResponse updateRole(UUID roleId, UpdateRoleRequest request);

    void deleteRole(UUID roleId);

    RoleResponse getRoleById(UUID roleId);

    RoleResponse getRoleByName(String name);

    List<RoleResponse> getAllRoles();
}
