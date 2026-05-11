package org.project.floodalert.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.model.UserAddress;
import org.project.floodalert.auth.repository.UserAddressRepository;
import org.project.floodalert.auth.service.StaticLocationRedisService;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaticLocationRedisServiceImpl implements StaticLocationRedisService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserAddressRepository userAddressRepository;
    
    private static final String GEO_LOCATIONS_KEY = "user_locations:static";
    
    private static final String ADDRESS_DETAILS_PREFIX = "address_details:";
    
    @Override
    public void addOrUpdateLocation(UUID userId, UUID addressId, String zoneName, Double lat, Double lon) {
        try {
            Point point = new Point(lon, lat);
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            geoOps.add(GEO_LOCATIONS_KEY, point, addressId.toString());
            
            String hashKey = ADDRESS_DETAILS_PREFIX + addressId;
            Map<String, String> addressDetails = new HashMap<>();
            addressDetails.put("userId", userId.toString());
            addressDetails.put("zoneName", zoneName);
            addressDetails.put("lat", lat.toString());
            addressDetails.put("lon", lon.toString());
            
            redisTemplate.opsForHash().putAll(hashKey, addressDetails);

            
        } catch (Exception e) {
            log.error("Lỗi khi thêm/cập nhật static location: addressId={}", addressId, e);
        }
    }
    
    @Override
    public void removeLocation(UUID userId, UUID addressId) {
        try {
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            geoOps.remove(GEO_LOCATIONS_KEY, addressId.toString());
            
            String hashKey = ADDRESS_DETAILS_PREFIX + addressId.toString();
            redisTemplate.delete(hashKey);

        } catch (Exception e) {
            log.error("Lỗi khi xóa static location: addressId={}", addressId, e);
        }
    }
    
    @Override
    public void removeAllUserLocations(UUID userId) {
        try {
            Set<String> keys = redisTemplate.keys(ADDRESS_DETAILS_PREFIX + "*");
            
            if (keys == null || keys.isEmpty()) {
                log.info("Không có location nào của user {}", userId);
                return;
            }
            
            int removedCount = 0;
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            
            for (String hashKey : keys) {
                Map<Object, Object> details = redisTemplate.opsForHash().entries(hashKey);
                String storedUserId = (String) details.get("userId");
                
                if (userId.toString().equals(storedUserId)) {
                    String addressId = hashKey.replace(ADDRESS_DETAILS_PREFIX, "");
                    
                    geoOps.remove(GEO_LOCATIONS_KEY, addressId);
                    
                    redisTemplate.delete(hashKey);
                    
                    removedCount++;
                }
            }
            
            log.info("Đã xóa {} static locations của user {}", removedCount, userId);
            
        } catch (Exception e) {
            log.error("Lỗi khi xóa tất cả static locations của user {}", userId, e);
        }
    }
    
    @Override
    public long syncAllAddressesToRedis() {
        try {
            log.info("========== BẮT ĐẦU SYNC ĐỊA CHỈ LÊN REDIS ==========");
            
            redisTemplate.delete(GEO_LOCATIONS_KEY);
            
            Set<String> oldHashKeys = redisTemplate.keys(ADDRESS_DETAILS_PREFIX + "*");
            if (oldHashKeys != null && !oldHashKeys.isEmpty()) {
                redisTemplate.delete(oldHashKeys);
                log.info("Đã xóa {} HASH keys cũ", oldHashKeys.size());
            }
            
            List<UserAddress> allAddresses = userAddressRepository.findAll();
            log.info("Tìm thấy {} địa chỉ trong database", allAddresses.size());
            
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            long syncCount = 0;

            for (UserAddress address : allAddresses) {
                try {
                    String zoneName = determineZoneName(address);
                    
                    Point point = new Point(
                            address.getLon().doubleValue(), 
                            address.getLat().doubleValue()
                    );
                    geoOps.add(GEO_LOCATIONS_KEY, point, address.getId().toString());
                    
                    String hashKey = ADDRESS_DETAILS_PREFIX + address.getId().toString();
                    Map<String, String> addressDetails = new HashMap<>();
                    addressDetails.put("userId", address.getUserId().toString());
                    addressDetails.put("zoneName", zoneName);
                    addressDetails.put("lat", address.getLat().toString());
                    addressDetails.put("lon", address.getLon().toString());
                    
                    redisTemplate.opsForHash().putAll(hashKey, addressDetails);

                } catch (Exception e) {
                    log.warn("Lỗi khi sync địa chỉ id={}: {}", address.getId(), e.getMessage());
                }
            }
            
            log.info("========== HOÀN TẤT SYNC ĐỊA CHỈ ==========");
            log.info("Đã sync {} địa chỉ lên Redis", syncCount);
            return syncCount;
            
        } catch (Exception e) {
            log.error("Lỗi khi sync tất cả địa chỉ lên Redis", e);
            return 0;
        }
    }

    private String determineZoneName(UserAddress address) {
        String typeVi = mapAddressTypeToVietnamese(address.getAddressType());
        String text = "";
        if (address.getAddressText() != null && !address.getAddressText().isBlank()) {
            text = address.getAddressText().trim();
            if (text.length() > 50) {
                text = text.substring(0, 50).trim() + "...";
            }
        }

        boolean hasType = !typeVi.isEmpty();
        boolean hasText = !text.isEmpty();

        if (hasType && hasText) {
            return typeVi + " - " + text;
        } else if (hasType) {
            return typeVi;
        } else if (hasText) {
            return text;
        }

        return "Địa điểm";
    }
    
    /**
     * Map address type sang tiếng Việt
     */
    private String mapAddressTypeToVietnamese(String addressType) {
        if (addressType == null || addressType.isBlank()) {
            return "";
        }
        return switch (addressType.toUpperCase()) {
            case "HOME" -> "Nhà riêng";
            case "WORK" -> "Công ty";
            case "SCHOOL" -> "Trường học";
            case "OTHER" -> "Khác";
            default -> addressType;
        };
    }
}
