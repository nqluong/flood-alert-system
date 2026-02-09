package org.project.floodalert.floodprocessor.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.project.floodalert.floodprocessor.exception.ProcessorErrorCode;
import org.project.floodalert.floodprocessor.utils.RedisKeyGenerator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisStateRepository {

    private final StringRedisTemplate stringRedisTemplate;

    public Map<String, FloodStatus> batchGetPreviousStatuses(Collection<String> sensorIds) {
        if (sensorIds == null || sensorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // Tạo danh sách Redis keys
            List<String> redisKeys = RedisKeyGenerator.lastStatusKeys(sensorIds);

            // Batch get từ Redis
            List<String> values = stringRedisTemplate.opsForValue().multiGet(redisKeys);

            if (values == null) {
                log.warn("Redis multiGet trả về null, trả về map rỗng");
                return Collections.emptyMap();
            }

            // Map keys sang values (convert String sang FloodStatus)
            Map<String, FloodStatus> resultMap = new HashMap<>();
            List<String> sensorIdList = new ArrayList<>(sensorIds);

            for (int i = 0; i < sensorIdList.size(); i++) {
                String sensorId = sensorIdList.get(i);
                String statusValue = values.get(i);

                FloodStatus status = parseFloodStatus(statusValue);
                resultMap.put(sensorId, status);
            }

            return resultMap;

        } catch (Exception e) {
            log.error("Lỗi khi fetch trạng thái cũ từ Redis cho {} sensors", sensorIds.size(), e);
            throw new AppException(ProcessorErrorCode.REDIS_OPERATION_FAILED);
        }
    }


    public void batchSaveNewStatuses(Map<String, FloodStatus> statusMap) {
        if (statusMap == null || statusMap.isEmpty()) {
            return;
        }

        try {
            // Convert Map<sensorId, FloodStatus> sang Map<RedisKey, String>
            Map<String, String> redisMap = statusMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> RedisKeyGenerator.lastStatusKey(entry.getKey()),
                            entry -> entry.getValue().name()
                    ));

            // Batch set vào Redis
            stringRedisTemplate.opsForValue().multiSet(redisMap);

        } catch (Exception e) {
            log.error("Lỗi khi lưu trạng thái mới vào Redis cho {} sensors", statusMap.size(), e);
            throw new AppException(ProcessorErrorCode.REDIS_OPERATION_FAILED);
        }
    }

    private FloodStatus parseFloodStatus(String statusValue) {
        if (statusValue == null || statusValue.trim().isEmpty()) {
            return null;
        }

        try {
            return FloodStatus.valueOf(statusValue.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Không thể parse FloodStatus từ value: '{}', trả về null", statusValue);
            return null;
        }
    }
}
