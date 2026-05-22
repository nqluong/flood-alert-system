package org.project.floodalert.floodcore.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.config.RedisKeyProperties;
import org.project.floodalert.floodcore.dto.request.SensorFilterRequest;
import org.project.floodalert.floodcore.dto.response.SensorDetailResponse;
import org.project.floodalert.floodcore.dto.response.SensorMapResponse;
import org.project.floodalert.floodcore.dto.response.SensorSummaryResponse;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.service.CacheService;
import org.project.floodalert.floodcore.service.SensorRedisCache;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorRedisCacheImpl implements SensorRedisCache {

    private final CacheService cacheService;
    private final RedisKeyProperties redisKeyProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void syncSensorToRedis(Sensor sensor) {
        try {
            log.info("Bắt đầu đồng bộ sensor {} vào Redis", sensor.getSensorId());
            saveSensorMetadata(sensor);
            saveSensorLocation(sensor);
            syncBlacklistStatus(sensor.getSensorId(), sensor.getStatus());
            log.info("Đồng bộ sensor {} vào Redis thành công", sensor.getSensorId());
        } catch (Exception e) {
            log.error("Đồng bộ sensor {} vào Redis thất bại: {}",
                    sensor.getSensorId(), e.getMessage());
            throw new AppException(CoreErrorCode.REDIS_SYNC_FAILED, e.getMessage());
        }
    }

    @Override
    public void saveSensorMetadata(Sensor sensor) {
        String key = redisKeyProperties.getKeys().getSensor().getInfoKey(sensor.getSensorId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", sensor.getId().toString());
        metadata.put("sensor_id", sensor.getSensorId());
        metadata.put("name", sensor.getName());
        metadata.put("location_name", sensor.getLocationName());
        metadata.put("lat", sensor.getLat().toString());
        metadata.put("lon", sensor.getLon().toString());
        metadata.put("status", sensor.getStatus());
        metadata.put("api_key", sensor.getApiKey());
        metadata.put("warning_threshold", sensor.getWarningThreshold().toString());
        metadata.put("danger_threshold", sensor.getDangerThreshold().toString());

        if (sensor.getHardwareModel() != null) {
            metadata.put("hardware_model", sensor.getHardwareModel());
        }
        if (sensor.getFirmwareVersion() != null) {
            metadata.put("firmware_version", sensor.getFirmwareVersion());
        }

        cacheService.hSetAll(key, metadata);
    }

    @Override
    public void saveSensorLocation(Sensor sensor) {
        String geoKey = redisKeyProperties.getKeys().getMaps().getAllSensors();

        Point location = new Point(
                sensor.getLon().doubleValue(),
                sensor.getLat().doubleValue()
        );

        cacheService.geoAdd(geoKey, location, sensor.getSensorId());
    }

    @Override
    public void removeSensorFromRedis(String sensorId) {
        try {
            log.info("Xóa dữ liệu sensor {} khỏi Redis", sensorId);

            // Xóa metadata
            String infoKey = redisKeyProperties.getKeys().getSensor().getInfoKey(sensorId);
            cacheService.delete(infoKey);

            // Xóa khỏi Geo
            String geoKey = redisKeyProperties.getKeys().getMaps().getAllSensors();
            cacheService.geoRemove(geoKey, sensorId);
            addToBlacklist(sensorId);
            log.info("Đã xóa dữ liệu sensor {} khỏi Redis", sensorId);
        } catch (Exception e) {
            log.error("Lỗi khi xóa sensor {} khỏi Redis: {}",
                    sensorId, e.getMessage(), e);
            throw new AppException(CoreErrorCode.REDIS_SYNC_FAILED, e.getMessage());
        }
    }

    @Override
    public void updateSensorStatus(String sensorId, String status) {
        try {
            String key = redisKeyProperties.getKeys().getSensor().getInfoKey(sensorId);
            cacheService.hSet(key, "status", status);

            log.debug("Đã cập nhật status sensor {} trong Redis: {}", sensorId, status);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật status sensor {} trong Redis: {}",
                    sensorId, e.getMessage(), e);
            throw new AppException(CoreErrorCode.REDIS_SYNC_FAILED, e.getMessage());
        }
    }

    @Override
    public Optional<PageResponse<SensorSummaryResponse>> getCachedSensorList(SensorFilterRequest filter) {
        try {
            String cacheKey = buildListCacheKey(filter);
            Object cached = cacheService.get(cacheKey);

            if (cached != null) {
                log.debug("Cache HIT cho danh sách sensor: {}", cacheKey);
                PageResponse<SensorSummaryResponse> response = objectMapper.convertValue(
                        cached,
                        new TypeReference<PageResponse<SensorSummaryResponse>>() {}
                );
                return Optional.of(response);
            }

            log.debug("Cache MISS cho danh sách sensor: {}", cacheKey);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Lỗi khi lấy cache danh sách sensor: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void cacheSensorList(SensorFilterRequest filter, PageResponse<SensorSummaryResponse> response) {
        try {
            String cacheKey = buildListCacheKey(filter);
            long ttl = redisKeyProperties.getTtl().getSensorList();

            cacheService.set(cacheKey, response, ttl, TimeUnit.SECONDS);
            log.debug("Đã cache danh sách sensor: {}, TTL: {}s", cacheKey, ttl);

        } catch (Exception e) {
            log.error("Lỗi khi cache danh sách sensor: {}", e.getMessage(), e);
        }
    }

    @Override
    public Optional<SensorDetailResponse> getCachedSensorDetail(UUID sensorId) {
        try {
            String cacheKey = redisKeyProperties.getKeys().getCache().getSensorDetailKey(sensorId.toString());
            Object cached = cacheService.get(cacheKey);

            if (cached != null) {
                log.debug("Cache HIT cho chi tiết sensor: {}", sensorId);
                SensorDetailResponse response = objectMapper.convertValue(
                        cached,
                        SensorDetailResponse.class
                );
                return Optional.of(response);
            }

            log.debug("Cache MISS cho chi tiết sensor: {}", sensorId);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Lỗi khi lấy cache chi tiết sensor {}: {}", sensorId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void cacheSensorDetail(UUID sensorId, SensorDetailResponse response) {
        try {
            String cacheKey = redisKeyProperties.getKeys().getCache().getSensorDetailKey(sensorId.toString());
            long ttl = redisKeyProperties.getTtl().getSensorDetail();

            cacheService.set(cacheKey, response, ttl, TimeUnit.SECONDS);
            log.debug("Đã cache chi tiết sensor: {}, TTL: {}s", sensorId, ttl);

        } catch (Exception e) {
            log.error("Lỗi khi cache chi tiết sensor {}: {}", sensorId, e.getMessage(), e);
        }
    }

    @Override
    public Optional<SensorMapResponse> getCachedMapData() {
        try {
            String cacheKey = redisKeyProperties.getKeys().getCache().getMapAll();
            Object cached = cacheService.get(cacheKey);

            if (cached != null) {
                log.debug("Cache HIT cho map data");
                SensorMapResponse response = objectMapper.convertValue(
                        cached,
                        SensorMapResponse.class
                );
                return Optional.of(response);
            }

            log.debug("Cache MISS cho map data");
            return Optional.empty();

        } catch (Exception e) {
            log.error("Lỗi khi lấy cache map data: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void cacheMapData(SensorMapResponse response) {
        try {
            String cacheKey = redisKeyProperties.getKeys().getCache().getMapAll();
            long ttl = redisKeyProperties.getTtl().getMapCache();

            cacheService.set(cacheKey, response, ttl, TimeUnit.SECONDS);
            log.debug("Đã cache map data, TTL: {}s", ttl);

        } catch (Exception e) {
            log.error("Lỗi khi cache map data: {}", e.getMessage(), e);
        }
    }

    @Override
    public void evictSensorCache(UUID sensorId) {
        try {
            String detailKey = redisKeyProperties.getKeys().getCache().getSensorDetailKey(sensorId.toString());
            cacheService.delete(detailKey);

            // Xóa luôn list cache vì sensor có thể xuất hiện trong list
            evictAllListCache();

            log.info("Đã xóa cache cho sensor: {}", sensorId);

        } catch (Exception e) {
            log.error("Lỗi khi xóa cache sensor {}: {}", sensorId, e.getMessage(), e);
        }
    }

    @Override
    public void evictAllListCache() {
        try {
            String pattern = redisKeyProperties.getKeys().getCache().getSensorList() + "*";
            Set<String> keys = cacheService.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                List<String> keyList = new ArrayList<>(keys);
                cacheService.batchDelete(keyList);
                log.info("Đã xóa {} keys cache danh sách sensor", keys.size());
            }

        } catch (Exception e) {
            log.error("Lỗi khi xóa cache danh sách sensor: {}", e.getMessage(), e);
        }
    }

    @Override
    public void evictMapCache() {
        try {
            String cacheKey = redisKeyProperties.getKeys().getCache().getMapAll();
            cacheService.delete(cacheKey);
            log.info("Đã xóa cache map data");

        } catch (Exception e) {
            log.error("Lỗi khi xóa cache map data: {}", e.getMessage(), e);
        }
    }

    @Override
    public void addToBlacklist(String sensorId) {
        try {
            String blacklistKey = redisKeyProperties.getKeys().getSensor().getBlacklist();
            cacheService.sAdd(blacklistKey, sensorId);
            log.info("Đã thêm sensor {} vào blacklist. Ingestion sẽ drop data từ sensor này.",
                    sensorId);
        } catch (Exception e) {
            log.error("Lỗi khi thêm sensor {} vào blacklist: {}",
                    sensorId, e.getMessage(), e);
        }
    }

    @Override
    public void removeFromBlacklist(String sensorId) {
        try {
            String blacklistKey = redisKeyProperties.getKeys().getSensor().getBlacklist();
            Long removed = cacheService.sRemove(blacklistKey, sensorId);
            if (removed != null && removed > 0) {
                log.info("Đã xóa sensor {} khỏi blacklist. Ingestion có thể nhận data.",
                        sensorId);
            } else {
                log.debug("Sensor {} không có trong blacklist", sensorId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa sensor {} khỏi blacklist: {}",
                    sensorId, e.getMessage(), e);
        }
    }

    @Override
    public boolean isInBlacklist(String sensorId) {
        try {
            String blacklistKey = redisKeyProperties.getKeys().getSensor().getBlacklist();
            Boolean isMember = cacheService.sIsMember(blacklistKey, sensorId);
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("Lỗi khi check blacklist cho sensor {}: {}",
                    sensorId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void syncBlacklistStatus(String sensorId, String status) {
        if (isBlacklistStatus(status)) {
            addToBlacklist(sensorId);
        } else {
            // Các status có thể nhận data
            removeFromBlacklist(sensorId);
        }
    }

    private boolean isBlacklistStatus(String status) {
        return "DISABLED".equals(status) ||
                "MAINTENANCE".equals(status) ||
                "DELETED".equals(status) ||
                "OFFLINE".equals(status);
    }

    private String buildListCacheKey(SensorFilterRequest filter) {
        StringBuilder key = new StringBuilder(redisKeyProperties.getKeys().getCache().getSensorList());
        key.append(":page=").append(filter.getPage());
        key.append(":size=").append(filter.getSize());

        if (filter.getStatus() != null) {
            key.append(":status=").append(filter.getStatus());
        }
        if (filter.getSearch() != null) {
            key.append(":search=").append(filter.getSearch());
        }
        if (filter.getSortBy() != null) {
            key.append(":sort=").append(filter.getSortBy())
                    .append(":").append(filter.getSortDirection());
        }

        return key.toString();
    }
}
