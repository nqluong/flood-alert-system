package org.project.floodalert.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.UserGeoDTO;
import org.project.floodalert.notification.service.RedisGeoService;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisGeoServiceImpl implements RedisGeoService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_LOCATIONS_KEY = "user:active_locations";
    private static final String HEARTBEAT_PREFIX = "user:heartbeat:";

    /**
     * Tìm users gần điểm ngập lụt trong bán kính chỉ định
     * <p>
     * Sử dụng Redis GEORADIUS command
     *
     * @param lat      Latitude của điểm ngập
     * @param lon      Longitude của điểm ngập
     * @param radiusKm Bán kính tìm kiếm (km)
     * @return List users trong bán kính với distance
     */
    @Override
    public List<UserGeoDTO> findUsersNear(Double lat, Double lon, Double radiusKm) {
        try {
            log.debug("Redis GEO Query: Finding users near ({}, {}) within {}km", lat, lon, radiusKm);

            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            Point center = new Point(lon, lat);
            Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
            Circle area = new Circle(center, radius);

            // Quét không gian
            GeoResults<RedisGeoCommands.GeoLocation<Object>> results = geoOps.radius(
                    USER_LOCATIONS_KEY,
                    area,
                    RedisGeoCommands.GeoRadiusCommandArgs
                            .newGeoRadiusArgs()
                            .includeDistance()
                            .sortAscending()
            );

            if (results == null || results.getContent().isEmpty()) {
                return new ArrayList<>();
            }

            List<GeoResult<RedisGeoCommands.GeoLocation<Object>>> rawList = results.getContent();

            List<String> heartbeatKeys = rawList.stream()
                    .map(result -> HEARTBEAT_PREFIX + result.getContent().getName().toString())
                    .toList();

            List<Object> heartbeats = redisTemplate.opsForValue().multiGet(heartbeatKeys);

            List<UserGeoDTO> validUsers = new ArrayList<>();
            List<Object> ghostUsersToClean = new ArrayList<>();

            for (int i = 0; i < rawList.size(); i++) {
                GeoResult<RedisGeoCommands.GeoLocation<Object>> result = rawList.get(i);
                String userIdStr = result.getContent().getName().toString();

                if (heartbeats != null && heartbeats.get(i) != null) {
                    try {
                        UUID userId = UUID.fromString(userIdStr);
                        Double distanceMeters = result.getDistance().getValue() * 1000;

                        validUsers.add(UserGeoDTO.builder()
                                .userId(userId)
                                .distance(distanceMeters)
                                .build());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid userId format in Redis GEO: {}", userIdStr);
                    }
                } else {
                    ghostUsersToClean.add(userIdStr);
                }
            }

            if (!ghostUsersToClean.isEmpty()) {
                geoOps.remove(USER_LOCATIONS_KEY, ghostUsersToClean.toArray());
            }

            return validUsers;

        } catch (Exception e) {
            log.error("Error querying Redis GEO for location ({}, {})", lat, lon, e);
            return new ArrayList<>();
        }
    }

    public Point getUserLocation(UUID userId) {
        try {
            // Check heartbeat
            Object heartbeat = redisTemplate.opsForValue().get(HEARTBEAT_PREFIX + userId.toString());
            if (heartbeat == null) {
                return null;
            }

            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            List<Point> positions = geoOps.position(USER_LOCATIONS_KEY, userId.toString());

            if (positions != null && !positions.isEmpty()) {
                return positions.get(0);
            }
            return null;

        } catch (Exception e) {
            log.error("Error getting user location from Redis", e);
            return null;
        }
    }

    public Long getTotalUserCount() {
        try {
            return redisTemplate.opsForZSet().size(USER_LOCATIONS_KEY);
        } catch (Exception e) {
            log.error("Error getting total user count from Redis", e);
            return 0L;
        }
    }
}
