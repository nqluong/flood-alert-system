package org.project.floodalert.auth.service.impl;

import org.project.floodalert.auth.dto.request.CreateRoleRequest;
import org.project.floodalert.auth.dto.request.UpdateRoleRequest;
import org.project.floodalert.auth.exception.AuthErrorCode;
import org.project.floodalert.auth.model.Role;
import org.project.floodalert.common.exception.AppException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RoleValidator {
    public void validateCreateRequest(CreateRoleRequest request) {
        if (request == null) {
            throw new AppException(AuthErrorCode.INVALID_ROLE_DATA);
        }

        if (!StringUtils.hasText(request.getName())) {
            throw new AppException(AuthErrorCode.INVALID_ROLE_DATA);
        }

        if (request.getName().length() < 3 || request.getName().length() > 50) {
            throw new AppException(AuthErrorCode.INVALID_ROLE_DATA);
        }

        // Validate tên role không chứa ký tự đặc biệt
        if (!request.getName().matches("^[a-zA-Z0-9_-]+$")) {
            throw new AppException(AuthErrorCode.INVALID_ROLE_DATA);
        }
    }

    public void validateUpdateRequest(UpdateRoleRequest request, Role existingRole) {
        if (request == null) {
            throw new AppException(AuthErrorCode.INVALID_ROLE_DATA);
        }

        if (request.getName() != null) {
            if (request.getName().length() < 3 || request.getName().length() > 50) {
                throw new AppException(AuthErrorCode.INVALID_ROLE_DATA);
            }

            if (!request.getName().matches("^[a-zA-Z0-9_-]+$")) {
                throw new AppException(AuthErrorCode.INVALID_ROLE_DATA);
            }
        }

        if (request.getDescription() != null && request.getDescription().length() > 255) {
            throw new AppException(AuthErrorCode.INVALID_ROLE_DATA);
        }
    }
}
