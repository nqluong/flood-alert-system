package org.project.floodalert.auth.service;

import org.project.floodalert.auth.dto.response.FirebaseUserInfo;

public interface FirebaseAuthService {
    FirebaseUserInfo verifyIdToken(String idToken);

    void validateProvider(String expectedProvider, String actualProvider);
}
