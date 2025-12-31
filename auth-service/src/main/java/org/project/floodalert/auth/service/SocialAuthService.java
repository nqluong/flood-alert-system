package org.project.floodalert.auth.service;

import org.project.floodalert.auth.dto.request.SocialLoginRequest;
import org.project.floodalert.auth.dto.response.LoginResponse;

public interface SocialAuthService {
    /**
     * Authenticate user with social provider (Google/Facebook) via Firebase
     */
    LoginResponse authenticateWithSocial(SocialLoginRequest request, String ipAddress, String userAgent);

}
