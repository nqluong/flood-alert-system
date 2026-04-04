package org.project.floodalert.auth.constants;

import java.util.UUID;


public final class RedisKeyConstants {
    
    private RedisKeyConstants() {
        // Prevent instantiation
    }

    public static final String USER_ACTIVE_LOCATIONS = "user:active_locations";

    private static final String USER_HEARTBEAT_PREFIX = "user:heartbeat:";

    public static final long USER_HEARTBEAT_TTL_SECONDS = 300L;

    public static String getUserHeartbeatKey(UUID userId) {
        return USER_HEARTBEAT_PREFIX + userId;
    }
}
