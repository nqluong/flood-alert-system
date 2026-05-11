package org.project.floodalert.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.service.UserLocationSyncService;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLocationSyncServiceImpl implements UserLocationSyncService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_LOCATIONS_KEY = "user:active_locations";
    private static final String HEARTBEAT_PREFIX = "user:heartbeat:";
    private static final Duration HEARTBEAT_TTL = Duration.ofHours(24); // 24 giờ

    @Override
    public void syncUserLocationToRedis(UUID userId, Double latitude, Double longitude) {
        try {
            if (latitude == null || longitude == null) {
                log.warn("Không thể đồng bộ vị trí user {} vào Redis: latitude hoặc longitude null", userId);
                return;
            }

            // Validate coordinates
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                log.warn("Tọa độ không hợp lệ cho user {}: lat={}, lon={}", userId, latitude, longitude);
                return;
            }

            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            
            // Lưu vị trí vào Redis Geo
            Point location = new Point(longitude, latitude);
            Long added = geoOps.add(USER_LOCATIONS_KEY, location, userId.toString());

            // Set heartbeat để đánh dấu user còn active
            String heartbeatKey = HEARTBEAT_PREFIX + userId.toString();
            redisTemplate.opsForValue().set(heartbeatKey, System.currentTimeMillis(), HEARTBEAT_TTL);

            log.info("Đã đồng bộ vị trí user {} vào Redis Geo: ({}, {}), added={}", 
                    userId, latitude, longitude, added);

        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ vị trí user {} vào Redis Geo", userId, e);
        }
    }

    @Override
    public void removeUserLocationFromRedis(UUID userId) {
        try {
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            
            // Xóa vị trí khỏi Redis Geo
            Long removed = geoOps.remove(USER_LOCATIONS_KEY, userId.toString());

            // Xóa heartbeat
            String heartbeatKey = HEARTBEAT_PREFIX + userId.toString();
            redisTemplate.delete(heartbeatKey);

            log.info("Đã xóa vị trí user {} khỏi Redis Geo, removed={}", userId, removed);

        } catch (Exception e) {
            log.error("Lỗi khi xóa vị trí user {} khỏi Redis Geo", userId, e);
        }
    }
}
