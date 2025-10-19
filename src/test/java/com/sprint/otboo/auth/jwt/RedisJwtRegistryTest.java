package com.sprint.otboo.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.auth.dto.JwtInformation;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class RedisJwtRegistryTest {

    @Autowired
    private JwtRegistry jwtRegistry;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Container
    private static final GenericContainer<?> redisContainer =
        new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    private static void setRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
    }

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    private JwtInformation createTestSession(String accessToken, String refreshToken) {
        UserDto userDto = new UserDto(UUID.randomUUID(), Instant.now(), "user1@test.com", "user1", Role.USER, null, false);
        return new JwtInformation(userDto, accessToken, refreshToken);
    }

    @Test
    @DisplayName("새로운 JWT 세션을 등록하면 Redis에 관련 데이터가 모두 저장된다")
    void register() {
        // given
        JwtInformation session = createTestSession("access-token-1", "refresh-token-1");
        UserDto userDto = session.userDto();

        // when
        jwtRegistry.register(session);

        // then
        String userSessionsKey = "sessions:" + userDto.id();
        String jwtInfoKey = "jwtinfo:" + session.refreshToken();

        Set<Object> userSessions = redisTemplate.opsForZSet().range(userSessionsKey, 0, -1);
        assertThat(userSessions).containsExactly("refresh-token-1");

        Object jwtInfo = redisTemplate.opsForValue().get(jwtInfoKey);
        assertThat(jwtInfo).isNotNull();

        assertThat(redisTemplate.opsForSet().isMember("access_tokens", "access-token-1")).isTrue();
        assertThat(redisTemplate.opsForSet().isMember("refresh_tokens", "refresh-token-1")).isTrue();
    }

    @Test
    @DisplayName("최대 세션 수를 초과하여 로그인하면 가장 오래된 세션이 삭제된다")
    void register_evictOldestSession() {
        // given
        JwtInformation oldestSession = createTestSession("access-token-old", "refresh-token-old");
        JwtInformation newSession = new JwtInformation(oldestSession.userDto(), "access-token-new", "refresh-token-new");
        UserDto userDto = oldestSession.userDto();
        jwtRegistry.register(oldestSession);

        // when
        jwtRegistry.register(newSession);

        // then
        String userSessionsKey = "sessions:" + userDto.id();
        Set<Object> userSessions = redisTemplate.opsForZSet().range(userSessionsKey, 0, -1);

        assertThat(userSessions).hasSize(1);
        assertThat(userSessions).containsExactly("refresh-token-new");

        assertThat(redisTemplate.hasKey("jwtinfo:refresh-token-old")).isFalse();
        assertThat(redisTemplate.opsForSet().isMember("access_tokens", "access-token-old")).isFalse();
    }

    @Test
    @DisplayName("등록된 토큰은 유효성 검사를 통과한다")
    void isTokenValid() {
        // given
        JwtInformation session = createTestSession("access-token-valid", "refresh-token-valid");
        jwtRegistry.register(session);

        // when
        boolean validAccessResult = jwtRegistry.isAccessTokenValid("access-token-valid");
        boolean validRefreshResult = jwtRegistry.isRefreshTokenValid("refresh-token-valid");
        boolean invalidAccessResult = jwtRegistry.isAccessTokenValid("access-token-invalid");

        // then
        assertThat(validAccessResult).isTrue();
        assertThat(validRefreshResult).isTrue();
        assertThat(invalidAccessResult).isFalse();
    }

    @Test
    @DisplayName("invalidateAll 호출 시 특정 사용자의 모든 세션이 삭제된다")
    void invalidateAll() {
        // given
        JwtInformation session1 = createTestSession("access-1", "refresh-1");
        JwtInformation session2 = new JwtInformation(session1.userDto(), "access-2", "refresh-2");
        UserDto userDto = session1.userDto();
        jwtRegistry.register(session1);
        jwtRegistry.register(session2);

        // when
        jwtRegistry.invalidateAll(userDto.id());

        // then
        assertThat(redisTemplate.hasKey("sessions:" + userDto.id())).isFalse();
        assertThat(redisTemplate.hasKey("jwtinfo:refresh-1")).isFalse();
        assertThat(redisTemplate.hasKey("jwtinfo:refresh-2")).isFalse();
        assertThat(redisTemplate.opsForSet().isMember("access_tokens", "access-1")).isFalse();
        assertThat(redisTemplate.opsForSet().isMember("access_tokens", "access-2")).isFalse();
    }


}
