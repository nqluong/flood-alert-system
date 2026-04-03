package org.project.floodalert.notification.service;

import org.project.floodalert.notification.dto.UserGeoDTO;

import java.util.List;


public interface RedisGeoService {

    /**
     * Find users near a location within a specified radius
     *
     * @param lat Latitude of the center point
     * @param lon Longitude of the center point
     * @param radiusKm Radius in kilometers
     * @return List of users within the radius with their distances
     */
    List<UserGeoDTO> findUsersNear(Double lat, Double lon, Double radiusKm);
}
