package org.project.floodalert.auth.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.config.RedisKeyProperties;
import org.project.floodalert.auth.model.UserProfile;
import org.project.floodalert.auth.repository.UserProfileRepository;
import org.project.floodalert.auth.service.ReputationCacheService;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.common.exception.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReputationCacheServiceImpl implements ReputationCacheService {

    RedisTemplate<String, Object> redisTemplate;
    RedisKeyProperties redisKeyProperties;
    UserProfileRepository userProfileRepository;

    @Override
    @Async("cacheExecutor")
    public void cacheAsync(UUID userId, int reputationScore) {
        try {
            redisTemplate.opsForValue().set(
                    buildKey(userId),
                    reputationScore,
                    redisKeyProperties.getTtl().getUser().getReputation(),
                    TimeUnit.SECONDS
            );
            log.debug("Cached reputation async: userId={}, score={}", userId, reputationScore);
        } catch (Exception e) {
            log.warn("Failed to cache reputation async for userId={}", userId, e);
        }
    }

    @Override
    public int getOrLoad(UUID userId) {
        String key = buildKey(userId);
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            log.debug("Cache hit reputation: userId={}", userId);
            return ((Number) cached).intValue();
        }

        log.debug("Cache miss reputation - loading from DB: userId={}", userId);
        int score = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
                .getReputationScore();

        try {
            redisTemplate.opsForValue().set(
                    key, score,
                    redisKeyProperties.getTtl().getUser().getReputation(),
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.warn("Failed to cache reputation on load for userId={}", userId, e);
        }

        return score;
    }

    @Override
    @Transactional
    public void updateAndCache(UUID userId, int delta) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        int newScore = Math.max(0, profile.getReputationScore() + delta);
        profile.setReputationScore(newScore);
        userProfileRepository.save(profile);

        log.info("Updated reputation in DB: userId={}, delta={}, newScore={}", userId, delta, newScore);

        cacheAsync(userId, newScore);
    }

    private String buildKey(UUID userId) {
        return redisKeyProperties.getKeys().getUser().getReputation()
                .replace("{userId}", userId.toString());
    }
}
