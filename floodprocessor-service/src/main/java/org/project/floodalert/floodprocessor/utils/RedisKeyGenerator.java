package org.project.floodalert.floodprocessor.utils;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.List;

@UtilityClass
public class RedisKeyGenerator {

    private static final String LAST_STATUS_PREFIX = "sensor:last_status:";

    public static String lastStatusKey(String sensorId) {
        if (sensorId == null || sensorId.trim().isEmpty()) {
            throw new IllegalArgumentException("sensorId không được null hoặc empty");
        }
        return LAST_STATUS_PREFIX + sensorId;
    }

    public static List<String> lastStatusKeys(Collection<String> sensorIds) {
        if (sensorIds == null) {
            throw new IllegalArgumentException("sensorIds không được null");
        }

        return sensorIds.stream()
                .map(RedisKeyGenerator::lastStatusKey)
                .toList();
    }
}
