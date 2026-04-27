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
        log.info("[DeviceTokenService] Lưu FCM token cho userId: {}", userId);
        
        NotificationPreference pref = preferenceRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("[DeviceTokenService] Tạo mới NotificationPreference cho userId: {}", userId);
                    NotificationPreference newPref = new NotificationPreference();
                    newPref.setUserId(userId);
                    return newPref;
                });

        pref.setFcmToken(token);
        preferenceRepository.save(pref);

        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + userId, token);
        log.info("[DeviceTokenService] Đã lưu FCM token vào DB và Redis cho userId: {}", userId);
    }

    @Override
    @Transactional
    public void removeToken(UUID userId) {
        log.info("[DeviceTokenService] Xóa FCM token cho userId: {}", userId);
        
        preferenceRepository.findById(userId).ifPresent(pref -> {
            pref.setFcmToken(null);
            preferenceRepository.save(pref);
            log.info("[DeviceTokenService] Đã xóa FCM token khỏi DB cho userId: {}", userId);
        });

        redisTemplate.delete(REDIS_KEY_PREFIX + userId);
        log.info("[DeviceTokenService] Đã xóa FCM token khỏi Redis cho userId: {}", userId);
    }

    @Override
    @Transactional
    public void handleFcmTokenEvent(UUID userId, String token, Boolean isActive) {
        log.info("[DeviceTokenService] Xử lý FCM token event - userId: {}, isActive: {}", userId, isActive);

        if (Boolean.TRUE.equals(isActive)) {
            saveToken(userId, token);
            log.info("[DeviceTokenService] Đã lưu FCM token cho userId: {}", userId);
        } else {
            removeToken(userId);
            log.info("[DeviceTokenService] Đã xóa FCM token cho userId: {}", userId);
        }
    }
}
