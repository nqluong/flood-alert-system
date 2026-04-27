package org.project.floodalert.auth.service;

import org.project.floodalert.auth.dto.response.UserListResponse;
import org.project.floodalert.common.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface UserManagementService {
    PageResponse<UserListResponse> getAllUsers(Pageable pageable);
}
