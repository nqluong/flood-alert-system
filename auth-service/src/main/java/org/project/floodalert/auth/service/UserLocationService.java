package org.project.floodalert.auth.service;

import java.util.UUID;


public interface UserLocationService {

    void updateLocation(UUID userId, Double latitude, Double longitude);
}
