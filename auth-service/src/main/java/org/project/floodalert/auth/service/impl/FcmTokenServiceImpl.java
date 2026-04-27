package org.project.floodalert.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.dto.event.FcmTokenEvent;
import org.project.floodalert.auth.dto.request.FcmTokenRequest;
import org.project.floodalert.auth.enums.DeviceType;
import org.project.floodalert.auth.model.FcmToken;
import org.project.floodalert.auth.repository.FcmTokenRepository;
import org.project.floodalert.auth.service.FcmTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenServiceImpl implements FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.fcm-token}")
    private String fcmTokenTopic;

    @Override
    @Transactional
    public void upsertToken(UUID userId, FcmTokenRequest request) {
        log.info("[FcmTokenService] Upsert token cho userId: {}, deviceId: {}", userId, request.getDeviceId());

        FcmToken fcmToken = fcmTokenRepository.findByUserIdAndDeviceId(userId, request.getDeviceId())
                .orElse(FcmToken.builder()
                        .userId(userId)
                        .deviceId(request.getDeviceId())
                        .build());

        fcmToken.setToken(request.getToken());
        fcmToken.setDeviceType(DeviceType.valueOf(request.getDeviceType().toUpperCase()));
        fcmToken.setDeviceName(request.getDeviceName());
        fcmToken.setActive(true);
        fcmToken.setLastUsedAt(LocalDateTime.now());

        fcmTokenRepository.save(fcmToken);

        FcmTokenEvent event = FcmTokenEvent.builder()
                .userId(userId)
                .token(request.getToken())
                .isActive(true)
                .eventType("UPSERT")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(fcmTokenTopic, userId.toString(), event);
        log.info("[FcmTokenService] Đã gửi UPSERT event lên Kafka cho userId: {}", userId);
    }

    @Override
    @Transactional
    public void deactivateToken(UUID userId, String deviceId) {
        log.info("[FcmTokenService] Deactivate token cho userId: {}, deviceId: {}", userId, deviceId);

        fcmTokenRepository.findByUserIdAndDeviceId(userId, deviceId)
                .ifPresent(token -> {
                    token.setActive(false);
                    fcmTokenRepository.save(token);

                    FcmTokenEvent event = FcmTokenEvent.builder()
                            .userId(userId)
                            .token(token.getToken())
                            .isActive(false)
                            .eventType("DEACTIVATE")
                            .timestamp(LocalDateTime.now())
                            .build();

                    kafkaTemplate.send(fcmTokenTopic, userId.toString(), event);
                    log.info("[FcmTokenService] Đã gửi DEACTIVATE event lên Kafka cho userId: {}, deviceId: {}", 
                            userId, deviceId);
                });
    }

    @Override
    @Transactional
    public void deactivateAllTokens(UUID userId) {
        log.info("[FcmTokenService] Deactivate tất cả tokens cho userId: {}", userId);

        List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);
        
        if (!tokens.isEmpty()) {
            fcmTokenRepository.deactivateAllByUserId(userId);

            tokens.forEach(token -> {
                FcmTokenEvent event = FcmTokenEvent.builder()
                        .userId(userId)
                        .token(token.getToken())
                        .isActive(false)
                        .eventType("DEACTIVATE_ALL")
                        .timestamp(LocalDateTime.now())
                        .build();

                kafkaTemplate.send(fcmTokenTopic, userId.toString(), event);
            });

            log.info("[FcmTokenService] Đã gửi {} DEACTIVATE_ALL events lên Kafka cho userId: {}", 
                    tokens.size(), userId);
        }
    }

    @Override
    @Transactional
    public void cleanUpStaleTokens() {
        log.info("[FcmTokenService] Bắt đầu dọn dẹp stale tokens");

        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        List<FcmToken> staleTokens = fcmTokenRepository.findStaleTokens(threshold);

        if (!staleTokens.isEmpty()) {
            staleTokens.forEach(token -> {
                token.setActive(false);
                fcmTokenRepository.save(token);

                // Gửi event lên Kafka
                FcmTokenEvent event = FcmTokenEvent.builder()
                        .userId(token.getUserId())
                        .token(token.getToken())
                        .isActive(false)
                        .eventType("CLEANUP")
                        .timestamp(LocalDateTime.now())
                        .build();

                kafkaTemplate.send(fcmTokenTopic, token.getUserId().toString(), event);
            });

            log.info("[FcmTokenService] Đã dọn dẹp {} stale tokens và gửi events lên Kafka", staleTokens.size());
        } else {
            log.info("[FcmTokenService] Không có stale tokens cần dọn dẹp");
        }
    }
}
