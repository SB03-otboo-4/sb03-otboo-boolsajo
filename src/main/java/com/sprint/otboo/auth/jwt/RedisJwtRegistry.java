package com.sprint.otboo.auth.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.dto.JwtInformation;
import com.sprint.otboo.common.exception.auth.SerializationFailedException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

@Component
@Primary
@Slf4j
public class RedisJwtRegistry implements JwtRegistry {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final long refreshTokenValidityInSeconds;
    private final int maxActiveJwtCount;

    public RedisJwtRegistry(RedisTemplate<String, Object> redisTemplate,
        ObjectMapper objectMapper,
        @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidityInSeconds,
        @Value("${jwt.session.max-active-jwt}") int maxActiveJwtCount) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
        this.maxActiveJwtCount = maxActiveJwtCount;
    }

    private String userSessionsKey(UUID userId) { return "sessions:" + userId; }
    private String jwtInfoKey(String refreshToken) { return "jwtinfo:" + refreshToken; }
    private final String VALID_ACCESS_TOKENS_KEY = "access_tokens";
    private final String VALID_REFRESH_TOKENS_KEY = "refresh_tokens";


    @Override
    public void register(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.userDto().id();
        String userSessionsKey = userSessionsKey(userId);

        // 1. 최대 세션 수 초과 시 가장 오래된 세션 제거
        Long sessionCount = redisTemplate.opsForZSet().zCard(userSessionsKey);
        if (sessionCount != null && sessionCount >= maxActiveJwtCount) {
            // 가장 오래된 세션(score가 가장 낮은)의 refresh token 가져오기
            Set<Object> oldestSessions = redisTemplate.opsForZSet().range(userSessionsKey, 0, 0);
            if (oldestSessions != null && !oldestSessions.isEmpty()) {
                String oldestRefreshToken = (String) oldestSessions.iterator().next();
                invalidate(oldestRefreshToken); // 내부적으로 토큰 무효화 처리
            }
        }

        // 2. 새 세션 정보 등록
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                RedisOperations<String, Object> ops = (RedisOperations<String, Object>) operations;
                ops.multi();

                // 사용자 세션 Sorted Set에 추가 (score: 현재시간, value: refreshToken)
                ops.opsForZSet().add(userSessionsKey, jwtInformation.refreshToken(), System.currentTimeMillis());
                ops.expire(userSessionsKey, refreshTokenValidityInSeconds, TimeUnit.SECONDS);

                // 세션 상세 정보 저장
                try {
                    String jwtInfoJson = objectMapper.writeValueAsString(jwtInformation);
                    ops.opsForValue().set(jwtInfoKey(jwtInformation.refreshToken()), jwtInfoJson,
                        refreshTokenValidityInSeconds, TimeUnit.SECONDS);
                } catch (JsonProcessingException e) {
                    throw new SerializationFailedException(e);
                }

                // 유효한 토큰 Set에 추가
                ops.opsForSet().add(VALID_ACCESS_TOKENS_KEY, jwtInformation.accessToken());
                ops.opsForSet().add(VALID_REFRESH_TOKENS_KEY, jwtInformation.refreshToken());

                return ops.exec();
            }
        });
    }

    @Override
    public boolean isAccessTokenValid(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(VALID_ACCESS_TOKENS_KEY, accessToken));
    }

    @Override
    public boolean isRefreshTokenValid(String refreshToken) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(VALID_REFRESH_TOKENS_KEY, refreshToken));
    }

    @Override
    public void invalidate(String refreshToken) {
        // 1. Refresh Token으로 JwtInformation 조회
        String jwtInfoJson = (String) redisTemplate.opsForValue().get(jwtInfoKey(refreshToken));
        if (jwtInfoJson == null) return;

        try {
            JwtInformation jwtInfo = objectMapper.readValue(jwtInfoJson, JwtInformation.class);

            // 2. 관련된 모든 데이터 삭제
            redisTemplate.execute(new SessionCallback<>() {
                @Override
                public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                    RedisOperations<String, Object> ops = (RedisOperations<String, Object>) operations;
                    ops.multi();

                    ops.delete(jwtInfoKey(refreshToken));
                    ops.opsForZSet().remove(userSessionsKey(jwtInfo.userDto().id()), refreshToken);
                    ops.opsForSet().remove(VALID_ACCESS_TOKENS_KEY, jwtInfo.accessToken());
                    ops.opsForSet().remove(VALID_REFRESH_TOKENS_KEY, refreshToken);
                    return ops.exec();
                }
            });
        } catch (JsonProcessingException e) {
            log.error("JwtInformation 역직렬화 실패 Refresh Token: {}", refreshToken, e);
            throw new SerializationFailedException(e);
        }
    }

    @Override
    public void invalidateAll(UUID userId) {
        String userSessionsKey = userSessionsKey(userId);
        Set<Object> allRefreshTokens = redisTemplate.opsForZSet().range(userSessionsKey, 0, -1);

        if (allRefreshTokens != null) {
            allRefreshTokens.forEach(token -> invalidate((String) token));
        }
    }
}