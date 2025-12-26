package org.project.floodalert.auth.service;

import org.project.floodalert.auth.dto.request.AssignRoleRequest;
import org.project.floodalert.auth.dto.response.UserRoleResponse;

import java.util.List;
import java.util.UUID;

public interface UserRoleService {
    UserRoleResponse assignRole(AssignRoleRequest request);

    void removeRole(UUID userId, UUID roleId);

    List<UserRoleResponse> getUserRoles(UUID userId);

    List<UserRoleResponse> getRoleUsers(UUID roleId);
}
