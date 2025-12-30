package org.project.floodalert.auth.service;

import org.project.floodalert.auth.dto.request.UserProfileUpdateRequest;
import org.project.floodalert.auth.dto.response.UserProfileResponse;

import java.util.UUID;

public interface UserProfileService {
    UserProfileResponse getProfile(UUID userId);
    UserProfileResponse updateProfile(UUID userId, UserProfileUpdateRequest request);
}
