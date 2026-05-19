package org.project.floodalert.auth.service;

import org.project.floodalert.auth.dto.request.UserSearchRequest;
import org.project.floodalert.auth.dto.response.UserListResponse;
import org.project.floodalert.common.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserManagementService {
    PageResponse<UserListResponse> getAllUsers(Pageable pageable);

    PageResponse<UserListResponse> searchUsers(UserSearchRequest searchRequest, Pageable pageable);

    Map<String, String> getUserNamesByIds(List<UUID> userIds);
}
