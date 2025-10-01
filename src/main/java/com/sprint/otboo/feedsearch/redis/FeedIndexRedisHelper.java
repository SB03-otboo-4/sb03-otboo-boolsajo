package com.sprint.otboo.feedsearch.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * 피드 색인 배치를 위한 Redis 헬퍼
 *
 * <p>기능:
 * <ol>
 *   <li>분산 락: {@code SET NX EX} 패턴으로 단일 실행 보장. 토큰 기반 Lua 스크립트로 안전 해제</li>
 *   <li>커서 영속화: {@code updatedAt}, {@code id}를 키-값으로 저장/복원</li>
 * </ol>
 *
 * <p>키 네임스페이스:
 * <ul>
 *   <li>락: 호출 측에서 전달한 {@code key} 사용 (예: {@code locks:feed:index:{alias}})</li>
 *   <li>커서:
 *     <ul>
 *       <li>{@code feed:index:cursor:{name}:updatedAt}</li>
 *       <li>{@code feed:index:cursor:{name}:id}</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class FeedIndexRedisHelper {

    private final StringRedisTemplate redisTemplate;

    /**
     * 토큰 소유자만 락을 해제하도록 보장하는 Lua 스크립트
     */
    private static final String UNLOCK_LUA = """
        if redis.call('get', KEYS[1]) == ARGV[1] then
          return redis.call('del', KEYS[1])
        else
          return 0
        end
        """;

    /**
     * 분산 락 획득 시도
     *
     * @param key 락 키 (예: {@code locks:feed:index:{alias}})
     * @param ttl 락 만료 시간 (자동 해제 보장을 위해 필수)
     * @return 획득에 성공하면 락 토큰(UUID), 실패하면 {@code null}
     */
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    /**
     * 분산 락 해제
     *
     * <p>현재 키의 값(토큰)이 호출자가 보유한 토큰과 일치할 때만 삭제한다.</p>
     *
     * @param key   락 키
     * @param token 락 토큰 (null이면 무시)
     */
    public void unlock(String key, String token) {
        if (token == null) {
            return;
        }
        redisTemplate.execute(new DefaultRedisScript<>(UNLOCK_LUA, Long.class), List.of(key),
            token);
    }

    private String cursorUpdatedAtKey(String name) {
        return "feed:index:cursor:%s:updatedAt".formatted(name);
    }

    private String cursorIdKey(String name) {
        return "feed:index:cursor:%s:id".formatted(name);
    }

    /**
     * 커서를 저장한다
     *
     * @param name      네임스페이스/식별자 (보통 write-alias)
     * @param updatedAt 마지막 처리 문서의 {@code updatedAt}
     * @param id        마지막 처리 문서의 {@code id}
     */
    public void saveCursor(String name, Instant updatedAt, UUID id) {
        redisTemplate.opsForValue().set(cursorUpdatedAtKey(name), updatedAt.toString());
        redisTemplate.opsForValue().set(cursorIdKey(name), id.toString());
    }

    /**
     * 커서를 로드한다
     *
     * @param name 네임스페이스/식별자
     * @param out  결과를 담을 홀더 (null 허용 안 함)
     * @return 저장된 커서가 있으면 {@code true}, 없으면 {@code false}
     */
    public boolean loadCursor(String name, Holder out) {
        String updatedAtStr = redisTemplate.opsForValue().get(cursorUpdatedAtKey(name));
        String idStr = redisTemplate.opsForValue().get(cursorIdKey(name));
        if (updatedAtStr == null || idStr == null) {
            return false;
        }

        out.updatedAt = Instant.parse(updatedAtStr);
        out.id = UUID.fromString(idStr);
        return true;
    }

    /**
     * 커서를 EPOCH/0000-0000으로 초기화하여 저장한다.
     *
     * @param name 네임스페이스/식별자
     */
    public void resetCursor(String name) {
        saveCursor(name, Instant.EPOCH, new UUID(0, 0));
    }

    /**
     * 커서 전달용 홀더
     */
    public static class Holder {

        public Instant updatedAt;
        public UUID id;
    }
}
