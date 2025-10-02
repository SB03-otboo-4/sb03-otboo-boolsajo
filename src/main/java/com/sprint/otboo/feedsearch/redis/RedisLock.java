package com.sprint.otboo.feedsearch.redis;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * 토큰 기반 분산 락 (간단 버전): acquire / release / runWithLock
 */
@Component
@RequiredArgsConstructor
public class RedisLock {

    private final StringRedisTemplate redis;

    private static final DefaultRedisScript<Long> UNLOCK_LUA = new DefaultRedisScript<>(
        """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            else
              return 0
            end
            """, Long.class
    );

    /**
     * 락 시도: 성공 시 token, 실패 시 null
     */
    public String acquire(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redis.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /**
     * 비교 후 안전 해제
     */
    public void release(String key, String token) {
        if (token != null) {
            redis.execute(UNLOCK_LUA, List.of(key), token);
        }
    }

    /**
     * 락을 잡고 Runnable 실행 (못 잡으면 false 반환)
     */
    public boolean runWithLock(String key, Duration ttl, Runnable task) {
        String token = acquire(key, ttl);
        if (token == null) {
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            release(key, token);
        }
    }
}
