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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaticLocationRedisServiceImpl implements StaticLocationRedisService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserAddressRepository userAddressRepository;
    
    private static final String STATIC_LOCATIONS_KEY = "user:static_locations";
    private static final String MEMBER_SEPARATOR = "::";
    
    @Override
    public void addOrUpdateLocation(UUID userId, UUID addressId, String zoneName, Double lat, Double lon) {
        try {
            String member = buildMember(userId, addressId, zoneName);
            Point point = new Point(lon, lat);
            
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            
            Long added = geoOps.add(STATIC_LOCATIONS_KEY, point, member);
            
            if (added != null && added > 0) {
                log.info("Đã thêm static location vào Redis: user={}, addressId={}, zone={}, location=({}, {})",
                        userId, addressId, zoneName, lat, lon);
            } else {
                log.info("Đã cập nhật static location trong Redis: user={}, addressId={}, zone={}, location=({}, {})",
                        userId, addressId, zoneName, lat, lon);
            }
            
        } catch (Exception e) {
            log.error("Lỗi khi thêm/cập nhật static location vào Redis: user={}, addressId={}", 
                    userId, addressId, e);
        }
    }
    
    @Override
    public void removeLocation(UUID userId, UUID addressId) {
        try {
            String pattern = userId.toString() + "::" + addressId.toString() + "::*";
            
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            
            // Vì Redis Geo không hỗ trợ pattern matching trực tiếp,
            // ta phải dùng workaround: xóa bằng member name chính xác
            // Giả sử ta lưu addressId trong member để có thể xóa
            
            // Tạm thời xóa bằng cách scan all members (không hiệu quả cho production)
            // TODO: Cải thiện bằng cách lưu mapping addressId -> member trong Redis Hash
            
            List<Object> allMembers = getAllMembersForUser(userId);
            for (Object member : allMembers) {
                String memberStr = member.toString();
                if (memberStr.contains("::" + addressId.toString() + "::")) {
                    Long removed = geoOps.remove(STATIC_LOCATIONS_KEY, member);
                    if (removed != null && removed > 0) {
                        log.info("Đã xóa static location khỏi Redis: user={}, addressId={}, member={}",
                                userId, addressId, memberStr);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Lỗi khi xóa static location khỏi Redis: user={}, addressId={}", 
                    userId, addressId, e);
        }
    }
    
    @Override
    public void removeAllUserLocations(UUID userId) {
        try {
            List<Object> members = getAllMembersForUser(userId);
            
            if (!members.isEmpty()) {
                GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
                Long removed = geoOps.remove(STATIC_LOCATIONS_KEY, members.toArray());
                
                log.info("Đã xóa {} static locations của user {} khỏi Redis", removed, userId);
            }
            
        } catch (Exception e) {
            log.error("Lỗi khi xóa tất cả static locations của user {} khỏi Redis", userId, e);
        }
    }
    
    @Override
    public long syncAllAddressesToRedis() {
        try {
            log.info("Bắt đầu sync tất cả địa chỉ từ database lên Redis...");
            
            // Xóa toàn bộ key cũ
            redisTemplate.delete(STATIC_LOCATIONS_KEY);
            // Load tất cả địa chỉ từ database
            List<UserAddress> allAddresses = userAddressRepository.findAll();
            log.info("Tìm thấy {} địa chỉ trong database", allAddresses.size());
            
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            long syncCount = 0;

            for (UserAddress address : allAddresses) {
                try {
                    String zoneName = determineZoneName(address);
                    String member = buildMember(address.getUserId(), address.getId(), zoneName);
                    Point point = new Point(
                            address.getLon().doubleValue(), 
                            address.getLat().doubleValue()
                    );
                    
                    geoOps.add(STATIC_LOCATIONS_KEY, point, member);
                    syncCount++;
                    
                } catch (Exception e) {
                    log.warn("Lỗi khi sync địa chỉ id={}: {}", address.getId(), e.getMessage());
                }
            }
            
            return syncCount;
            
        } catch (Exception e) {
            log.error("Lỗi khi sync tất cả địa chỉ lên Redis", e);
            return 0;
        }
    }
    

    private String buildMember(UUID userId, UUID addressId, String zoneName) {
        return userId.toString() + MEMBER_SEPARATOR + addressId.toString() + MEMBER_SEPARATOR + zoneName;
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

        return "Địa điểm ";
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
    
    /**
     * Lấy tất cả members của một user
     * Workaround vì Redis Geo không hỗ trợ pattern matching
     */
    private List<Object> getAllMembersForUser(UUID userId) {
        // TODO: Implement efficient way to get user's members
        // Có thể dùng Redis Hash để lưu mapping: user:{userId}:addresses -> Set<member>
        // Hoặc scan toàn bộ members (không hiệu quả)
        
        // Tạm thời return empty list, cần implement sau
        log.warn("getAllMembersForUser chưa được implement hiệu quả, cần cải thiện");
        return List.of();
    }
}
