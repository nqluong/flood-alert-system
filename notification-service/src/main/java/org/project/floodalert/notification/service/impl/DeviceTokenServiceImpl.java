package org.project.floodalert.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.model.NotificationPreference;
import org.project.floodalert.notification.repository.NotificationPreferenceRepository;
import org.project.floodalert.notification.service.DeviceTokenService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {
    private final NotificationPreferenceRepository preferenceRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "user:fcm_token:";

    @Override
    public void saveToken(UUID userId, String token) {
        NotificationPreference pref = preferenceRepository.findById(userId)
                .orElseGet(() -> {
                    NotificationPreference newPref = new NotificationPreference();
                    newPref.setUserId(userId);
                    return newPref;
                });

        pref.setFcmToken(token);
        preferenceRepository.save(pref);

        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + userId, token);
    }

    @Override
    public void removeToken(UUID userId) {
        preferenceRepository.findById(userId).ifPresent(pref -> {
            pref.setFcmToken(null);
            preferenceRepository.save(pref);
        });

        redisTemplate.delete(REDIS_KEY_PREFIX + userId);
    }
}
