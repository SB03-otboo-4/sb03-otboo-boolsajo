package com.sprint.otboo.feedsearch.redis;

import com.sprint.otboo.feedsearch.dto.CursorDto;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 색인 커서 저장/복원 전용 헬퍼
 */
@Component
@RequiredArgsConstructor
public class FeedIndexRedisHelper {

    private final StringRedisTemplate redisTemplate;

    private String cursorUpdatedAtKey(String name) {
        return "feed:index:cursor:%s:updatedAt".formatted(name);
    }

    private String cursorIdKey(String name) {
        return "feed:index:cursor:%s:id".formatted(name);
    }

    public void saveCursor(String name, Instant updatedAt, UUID id) {
        redisTemplate.opsForValue().set(cursorUpdatedAtKey(name), updatedAt.toString());
        redisTemplate.opsForValue().set(cursorIdKey(name), id.toString());
    }


    public Optional<CursorDto> loadCursor(String name) {
        String updated = redisTemplate.opsForValue().get(cursorUpdatedAtKey(name));
        String id = redisTemplate.opsForValue().get(cursorIdKey(name));
        if (updated == null || id == null) return Optional.empty();
        return Optional.of(new CursorDto(Instant.parse(updated), UUID.fromString(id)));
    }
}
