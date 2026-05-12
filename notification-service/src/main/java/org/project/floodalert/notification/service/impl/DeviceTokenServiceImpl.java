package org.project.floodalert.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.model.NotificationPreference;
import org.project.floodalert.notification.repository.NotificationPreferenceRepository;
import org.project.floodalert.notification.service.DeviceTokenService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {
    private final NotificationPreferenceRepository preferenceRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "user:fcm_token:";

    @Override
    @Transactional
    public void saveToken(UUID userId, String token) {
        log.debug("[DeviceTokenService] Lưu FCM token cho userId: {}", userId);
        
        NotificationPreference pref = preferenceRepository.findById(userId)
                .orElseGet(() -> {
                    NotificationPreference newPref = new NotificationPreference();
                    newPref.setUserId(userId);
                    return newPref;
                });

        pref.setFcmToken(token);
        preferenceRepository.save(pref);

        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + userId, token);
        log.debug("[DeviceTokenService] Đã lưu FCM token vào DB và Redis cho userId: {}", userId);
    }

    @Override
    @Transactional
    public void removeToken(UUID userId) {
        log.debug("[DeviceTokenService] Xóa FCM token cho userId: {}", userId);

        preferenceRepository.findById(userId).ifPresent(pref -> {
            pref.setFcmToken(null);
            preferenceRepository.save(pref);
        });

        redisTemplate.delete(REDIS_KEY_PREFIX + userId);
        log.debug("[DeviceTokenService] Đã xóa FCM token khỏi Redis cho userId: {}", userId);
    }

    @Override
    @Transactional
    public void handleFcmTokenEvent(UUID userId, String token, Boolean isActive) {
        log.debug("[DeviceTokenService] Xử lý FCM token event - userId: {}, isActive: {}", userId, isActive);

        if (Boolean.TRUE.equals(isActive)) {
            saveToken(userId, token);
            log.debug("[DeviceTokenService] Đã lưu FCM token cho userId: {}", userId);
        } else {
            removeToken(userId);
            log.debug("[DeviceTokenService] Đã xóa FCM token cho userId: {}", userId);
        }
    }
}
