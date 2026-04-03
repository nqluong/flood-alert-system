package org.project.floodalert.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.UserGeoDTO;
import org.project.floodalert.notification.service.RedisGeoService;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Metrics;
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

    private static final String USER_LOCATIONS_KEY = "user:locations";

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
            log.debug("Redis GEO Query: Finding users near ({}, {}) within {}km",
                    lat, lon, radiusKm);

            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();

            Point center = new Point(lon, lat);

            Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
            Circle area = new Circle(center, radius);

            GeoResults<RedisGeoCommands.GeoLocation<Object>> results = geoOps.radius(
                    USER_LOCATIONS_KEY,
                    area,
                    RedisGeoCommands.GeoRadiusCommandArgs
                            .newGeoRadiusArgs()
                            .includeDistance()
                            .sortAscending() // Sort by distance ascending
            );

            if (results == null || results.getContent().isEmpty()) {
                log.debug("No users found within {}km radius", radiusKm);
                return new ArrayList<>();
            }

            // Convert results to UserGeoDTO
            List<UserGeoDTO> userGeoDTOs = new ArrayList<>();
            results.forEach(result -> {
                try {
                    String userIdStr = result.getContent().getName().toString();
                    Double distance = result.getDistance().getValue(); // in kilometers

                    // Convert distance from km to meters
                    Double distanceMeters = distance * 1000;

                    UUID userId = UUID.fromString(userIdStr);

                    userGeoDTOs.add(UserGeoDTO.builder()
                            .userId(userId)
                            .distance(distanceMeters)
                            .build());

                } catch (IllegalArgumentException e) {
                    log.warn("Invalid userId format in Redis: {}", result.getContent().getName());
                }
            });

            log.info("Found {} users within {}km radius", userGeoDTOs.size(), radiusKm);
            return userGeoDTOs;

        } catch (Exception e) {
            log.error("Error querying Redis GEO for location ({}, {})", lat, lon, e);
            return new ArrayList<>();
        }
    }

    /**
     * Optional: Add user location to Redis
     * <p>
     * Có thể gọi method này khi user cập nhật location
     * (Hoặc có thể implement trong user-service)
     *
     * @param userId User ID
     * @param lat    Latitude
     * @param lon    Longitude
     */
    public void addUserLocation(UUID userId, Double lat, Double lon) {
        try {
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();

            Point location = new Point(lon, lat);
            geoOps.add(USER_LOCATIONS_KEY, location, userId.toString());

            log.debug("Added user {} location to Redis: ({}, {})", userId, lat, lon);

        } catch (Exception e) {
            log.error("Error adding user location to Redis", e);
        }
    }

    /**
     * Optional: Remove user location from Redis
     *
     * @param userId User ID
     */
    public void removeUserLocation(UUID userId) {
        try {
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            geoOps.remove(USER_LOCATIONS_KEY, userId.toString());

            log.debug("Removed user {} location from Redis", userId);

        } catch (Exception e) {
            log.error("Error removing user location from Redis", e);
        }
    }

    /**
     * Optional: Get user location from Redis
     *
     * @param userId User ID
     * @return Point (lon, lat) or null if not found
     */
    public Point getUserLocation(UUID userId) {
        try {
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

    /**
     * Optional: Count total users in Redis GEO
     *
     * @return Total user count
     */
    public Long getTotalUserCount() {
        try {
            return redisTemplate.opsForZSet().size(USER_LOCATIONS_KEY);
        } catch (Exception e) {
            log.error("Error getting total user count from Redis", e);
            return 0L;
        }
    }
}
