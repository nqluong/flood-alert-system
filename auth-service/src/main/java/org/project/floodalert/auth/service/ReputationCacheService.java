package org.project.floodalert.auth.service;

import java.util.UUID;

public interface ReputationCacheService {

    void cacheAsync(UUID userId, int reputationScore);

    int getOrLoad(UUID userId);

    void updateAndCache(UUID userId, int delta);
}
