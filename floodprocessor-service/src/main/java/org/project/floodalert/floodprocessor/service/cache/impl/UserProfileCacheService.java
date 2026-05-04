package org.project.floodalert.floodprocessor.service.cache.impl;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.floodprocessor.client.AuthServiceClient;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class UserProfileCacheService {

    private static final String CACHE_KEY_PREFIX = "user:reputation:";
    private static final long CACHE_TTL_HOURS = 24L;
    private static final int DEFAULT_REPUTATION_SCORE = 50;

    private static final String LOG_CACHE_HIT = "[CACHE HIT]";
    private static final String LOG_CACHE_MISS = "[CACHE MISS]";
    private static final String LOG_REDIS_DOWN = "[REDIS DOWN]";
    private static final String LOG_AUTH_SERVICE_ERROR = "[AUTH_SERVICE_ERROR]";
    private static final String LOG_WRITE_BACK = "[WRITE_BACK]";
    private static final String LOG_WRITE_BACK_FAILED = "[WRITE_BACK FAILED]";
    private static final String LOG_FALLBACK = "[FALLBACK]";

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthServiceClient authServiceClient;

    public UserProfileCacheService(
            StringRedisTemplate stringRedisTemplate,
            AuthServiceClient authServiceClient) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.authServiceClient = authServiceClient;
    }

    public int getReputationScore(UUID userId) {
        try {
            Integer cachedScore = readFromRedisCache(userId);
            if (cachedScore != null) {
                log.info("{} userId={}, score={}", LOG_CACHE_HIT, userId, cachedScore);
                return cachedScore;
            }

            log.info("{} userId={}, calling auth-service", LOG_CACHE_MISS, userId);
            Integer apiScore = callAuthServiceWithFallback(userId);

            writeBackToRedisCache(userId, apiScore);

            return apiScore;

        } catch (RedisConnectionFailureException e) {
            log.warn("{} userId={}, redis connection failed: {}, fallback to layer 2",
                    LOG_REDIS_DOWN, userId, e.getMessage());
            return callAuthServiceWithFallback(userId);

        } catch (Exception e) {
            log.error("{} userId={}, unexpected error: {}, returning default score={}",
                    LOG_FALLBACK, userId, e.getMessage(), DEFAULT_REPUTATION_SCORE, e);
            return DEFAULT_REPUTATION_SCORE;
        }
    }


    private Integer readFromRedisCache(UUID userId) {
        try {
            String key = CACHE_KEY_PREFIX + userId;
            String cachedValue = stringRedisTemplate.opsForValue().get(key);

            if (cachedValue != null) {
                try {
                    return Integer.parseInt(cachedValue);
                } catch (NumberFormatException e) {
                    log.warn("[CACHE CORRUPTION] userId={}, invalid value in cache: {}", userId, cachedValue);
                    return null;
                }
            }
            return null;

        } catch (RedisConnectionFailureException e) {
            log.warn("{} userId={}, redis connection failed during read", LOG_REDIS_DOWN, userId);
            throw e;

        } catch (Exception e) {
            log.warn("[CACHE READ ERROR] userId={}, error: {}", userId, e.getMessage());
            return null;
        }
    }

    private Integer callAuthServiceWithFallback(UUID userId) {
        try {
            ApiResponse<Integer> response = authServiceClient.getReputation(userId);

            if (response != null && response.isSuccess() && response.getData() != null) {
                int score = response.getData();
                if (score >= 0 && score <= 100) {
                    log.debug("[AUTH_SERVICE_SUCCESS] userId={}, score={}", userId, score);
                    return score;
                } else {
                    log.warn("{} userId={}, invalid score from auth-service: {}, using default",
                            LOG_AUTH_SERVICE_ERROR, userId, score);
                    return DEFAULT_REPUTATION_SCORE;
                }
            } else {
                log.warn("{} userId={}, empty response from auth-service, using default",
                        LOG_AUTH_SERVICE_ERROR, userId);
                return DEFAULT_REPUTATION_SCORE;
            }

        } catch (feign.RetryableException e) {
            log.warn("{} userId={}, auth-service timeout/retry failed: {}", LOG_AUTH_SERVICE_ERROR, userId, e.getMessage());
            return DEFAULT_REPUTATION_SCORE;

        } catch (feign.FeignException.NotFound e) {
            log.warn("{} userId={}, user not found in auth-service, using default", LOG_AUTH_SERVICE_ERROR, userId);
            return DEFAULT_REPUTATION_SCORE;

        } catch (Exception e) {
            log.warn("{} userId={}, unexpected error calling auth-service: {}, using default",
                    LOG_AUTH_SERVICE_ERROR, userId, e.getMessage());
            return DEFAULT_REPUTATION_SCORE;
        }
    }


    private void writeBackToRedisCache(UUID userId, int score) {
        try {
            String key = CACHE_KEY_PREFIX + userId;
            Duration ttl = Duration.ofHours(CACHE_TTL_HOURS);
            stringRedisTemplate.opsForValue().set(key, String.valueOf(score), ttl);
            log.debug("{} userId={}, score={}, ttl={}h", LOG_WRITE_BACK, userId, score, CACHE_TTL_HOURS);

        } catch (RedisConnectionFailureException e) {
            log.warn("{} userId={}, redis connection failed: {}", LOG_WRITE_BACK_FAILED, userId, e.getMessage());

        } catch (Exception e) {
            log.warn("{} userId={}, error writing to redis: {}", LOG_WRITE_BACK_FAILED, userId, e.getMessage());
        }
    }
}
