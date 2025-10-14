package com.sprint.otboo.feedsearch.redis;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisLockHelper {

    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> UNLOCK_LUA = new DefaultRedisScript<>(
        """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            else
              return 0
            end
            """, Long.class
    );

    public String acquire(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    public void release(String key, String token) {
        if (token != null) {
            redisTemplate.execute(UNLOCK_LUA, List.of(key), token);
        }
    }

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
