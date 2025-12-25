package org.project.floodalert.auth.service;

import java.util.UUID;

public interface RoleService {

    void assignRoleToUser(UUID userId, String roleName);

    void assignRoleDefaultToUser(UUID userId);

    void assignRolesToUser(UUID userId, String[] roleNames);
}
