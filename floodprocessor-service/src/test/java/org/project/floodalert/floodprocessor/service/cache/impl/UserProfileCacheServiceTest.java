package org.project.floodalert.floodprocessor.service.cache.impl;

import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.floodprocessor.client.AuthServiceClient;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserProfileCacheService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void getReputationScore_cacheHit_returnsScore() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("75");

        service.getReputationScore(userId);
    }


    @Test
    void getReputationScore_cacheMiss_authServiceReturnsValidScore() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        ApiResponse<Integer> response = mockSuccessResponse(80);
        when(authServiceClient.getReputation(userId)).thenReturn(response);

        service.getReputationScore(userId);
    }


    @Test
    void getReputationScore_redisDownOnRead_fallsBackToAuthService() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("down"));

        ApiResponse<Integer> response = mockSuccessResponse(60);
        when(authServiceClient.getReputation(userId)).thenReturn(response);

        service.getReputationScore(userId);
    }

    @Test
    void getReputationScore_unexpectedException_returnsDefault() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenThrow(new RuntimeException("unexpected"));

        service.getReputationScore(userId);
    }


    @Test
    void getReputationScore_cacheValueInvalid_treatsAsMiss() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("not-a-number");

        ApiResponse<Integer> response = mockSuccessResponse(55);
        when(authServiceClient.getReputation(userId)).thenReturn(response);

        service.getReputationScore(userId);
    }


    @Test
    void getReputationScore_cacheReadThrowsGenericException_treatsAsMiss() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("generic read error"));

        service.getReputationScore(userId);
    }


    @Test
    void getReputationScore_authServiceScoreOutOfRange_returnsDefault() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        ApiResponse<Integer> response = mockSuccessResponse(150);
        when(authServiceClient.getReputation(userId)).thenReturn(response);

        service.getReputationScore(userId);
    }


    @Test
    void getReputationScore_authServiceReturnsNull_returnsDefault() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(authServiceClient.getReputation(userId)).thenReturn(null);

        service.getReputationScore(userId);
    }


    @Test
    void getReputationScore_authServiceResponseNotSuccess_returnsDefault() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        ApiResponse<Integer> response = mock(ApiResponse.class);
        when(response.isSuccess()).thenReturn(false);
        when(authServiceClient.getReputation(userId)).thenReturn(response);

        service.getReputationScore(userId);
    }


    @Test
    void getReputationScore_authServiceRetryableException_returnsDefault() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(authServiceClient.getReputation(userId))
                .thenThrow(mock(RetryableException.class));

        service.getReputationScore(userId);
    }

    @Test
    void getReputationScore_authServiceNotFound_returnsDefault() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(authServiceClient.getReputation(userId))
                .thenThrow(mock(FeignException.NotFound.class));

        service.getReputationScore(userId);
    }


    @Test
    void getReputationScore_authServiceGenericException_returnsDefault() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(authServiceClient.getReputation(userId))
                .thenThrow(new RuntimeException("service error"));

        service.getReputationScore(userId);
    }


    @Test
    void getReputationScore_writeBackRedisDown_logsWarnAndContinues() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        ApiResponse<Integer> response = mockSuccessResponse(70);
        when(authServiceClient.getReputation(userId)).thenReturn(response);

        doThrow(new RedisConnectionFailureException("write down"))
                .when(valueOperations).set(anyString(), anyString(), any());

        service.getReputationScore(userId);
    }


    @Test
    void getReputationScore_writeBackGenericError_logsWarnAndContinues() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        ApiResponse<Integer> response = mockSuccessResponse(70);
        when(authServiceClient.getReputation(userId)).thenReturn(response);

        doThrow(new RuntimeException("write error"))
                .when(valueOperations).set(anyString(), anyString(), any());

        service.getReputationScore(userId);
    }


    private ApiResponse<Integer> mockSuccessResponse(int score) {
        ApiResponse<Integer> response = mock(ApiResponse.class);
        when(response.isSuccess()).thenReturn(true);
        when(response.getData()).thenReturn(score);
        return response;
    }
}