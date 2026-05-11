package org.project.floodalert.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.constants.RedisKeyConstants;
import org.project.floodalert.auth.exception.RedisOperationException;
import org.project.floodalert.auth.service.UserLocationService;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLocationServiceImpl implements UserLocationService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void updateLocation(UUID userId, Double latitude, Double longitude) {
        log.debug("Updating location for user: {} at ({}, {})", userId, latitude, longitude);

        try {
            Point location = new Point(longitude, latitude);
            String userIdStr = userId.toString();
            String heartbeatKey = RedisKeyConstants.getUserHeartbeatKey(userId);

            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                connection.geoCommands().geoAdd(
                        RedisKeyConstants.USER_ACTIVE_LOCATIONS.getBytes(),
                        location,
                        userIdStr.getBytes()
                );

                connection.stringCommands().setEx(
                        heartbeatKey.getBytes(),
                        RedisKeyConstants.USER_HEARTBEAT_TTL_SECONDS,
                        "1".getBytes()
                );

                return null;
            });

            log.debug("Successfully updated location and heartbeat for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to update location for user: {} due to Redis error", userId, e);
            throw new RedisOperationException("Unable to update user location", e);
        }
    }
}
