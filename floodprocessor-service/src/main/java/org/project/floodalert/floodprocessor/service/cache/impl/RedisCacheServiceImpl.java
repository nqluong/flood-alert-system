package org.project.floodalert.floodprocessor.service.cache.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.config.RedisKeyProperties;
import org.project.floodalert.floodprocessor.exception.ProcessorErrorCode;
import org.project.floodalert.floodprocessor.service.cache.RedisCacheService;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements RedisCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyProperties redisKeyProperties;
    
    @Override
    public Map<String, Map<String, String>> bulkFetchSensorInfo(Set<String> sensorIds) {
        if (sensorIds == null || sensorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> keys = sensorIds.stream()
                    .map(id -> redisKeyProperties.getKeys().getSensor().getInfoKey(id))
                    .toList();
            
            // Sử dụng executePipelined để fetch tất cả HGETALL
            List<Object> results = redisTemplate.executePipelined(
                    (RedisCallback<Object>) connection -> {
                        keys.forEach(key -> {
                            try {
                                connection.hashCommands().hGetAll(key.getBytes());
                            } catch (Exception e) {
                                log.warn("Lỗi khi gọi hGetAll cho key: {}", key, e);
                            }
                        });
                        return null;
                    }
            );
            
            // Map kết quả theo sensor_id
            return getStringMapMap(sensorIds, results);
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure khi bulk fetch sensor info", e);
            throw new AppException(ProcessorErrorCode.REDIS_CONNECTION_FAILED);
            
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi bulk fetch từ Redis", e);
            throw new AppException(ProcessorErrorCode.REDIS_OPERATION_FAILED);
        }
    }

    private static Map<String, Map<String, String>> getStringMapMap(Set<String> sensorIds, List<Object> results) {
        Map<String, Map<String, String>> redisDataMap = new HashMap<>();
        List<String> sensorIdList = new ArrayList<>(sensorIds);

        for (int i = 0; i < sensorIdList.size(); i++) {
            String sensorId = sensorIdList.get(i);
            Object result = results.get(i);

            if (result instanceof Map<?, ?> map && !map.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, String> stringMap = (Map<String, String>) map;
                redisDataMap.put(sensorId, stringMap);
            }
        }
        return redisDataMap;
    }

    @Override
    public Map<String, String> getSensorInfo(String sensorId) {
        if (sensorId == null || sensorId.isEmpty()) {
            log.warn("Sensor ID không hợp lệ (null hoặc empty)");
            return null;
        }
        
        try {
            String key = redisKeyProperties.getKeys().getSensor().getInfoKey(sensorId);
            Map<Object, Object> rawData = redisTemplate.opsForHash().entries(key);
            
            if (rawData == null || rawData.isEmpty()) {
                log.debug("Không tìm thấy thông tin cho sensor {} trong Redis", sensorId);
                return null;
            }
            
            // Convert Map<Object, Object> sang Map<String, String>
            Map<String, String> result = new HashMap<>();
            rawData.forEach((k, v) -> {
                if (k != null && v != null) {
                    result.put(k.toString(), v.toString());
                }
            });
            
            return result;
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure khi get sensor info cho sensor {}", sensorId, e);
            throw new AppException(ProcessorErrorCode.REDIS_CONNECTION_FAILED);
            
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi get sensor info cho sensor {}", sensorId, e);
            throw new AppException(ProcessorErrorCode.REDIS_OPERATION_FAILED);
        }
    }
    
    @Override
    public boolean isRedisAvailable() {
        try {
            // Ping Redis để kiểm tra connection
            String result = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            
            boolean available = "PONG".equalsIgnoreCase(result);
            
            if (available) {
                log.debug("Redis connection OK");
            } else {
                log.warn("Redis ping không trả về PONG, result: {}", result);
            }
            
            return available;
            
        } catch (Exception e) {
            log.error("Không thể kết nối tới Redis", e);
            return false;
        }
    }
}
