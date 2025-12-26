package org.project.floodalert.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.project.floodalert.auth.exception.AuthErrorCode;
import org.project.floodalert.auth.repository.RoleRepository;
import org.project.floodalert.auth.repository.UserRepository;
import org.project.floodalert.common.exception.AppException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRoleValidator {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public void validateAssignRequest(UUID userId, UUID roleId) {
        if (userId == null || roleId == null) {
            throw new AppException(AuthErrorCode.INVALID_ROLE_DATA);
        }
    }
}
