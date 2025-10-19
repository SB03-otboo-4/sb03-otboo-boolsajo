package com.sprint.otboo.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.dto.JwtInformation;
import com.sprint.otboo.common.config.RedisConfig;
import com.sprint.otboo.common.exception.auth.SerializationFailedException;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@DataRedisTest
@Import({RedisJwtRegistry.class, RedisConfig.class})
class RedisJwtRegistryExceptionTest {

    @Autowired
    private JwtRegistry jwtRegistry;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private ObjectMapper mockObjectMapper;

    @Container
    private static final GenericContainer<?> redisContainer =
        new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    static {
        redisContainer.start();
        System.setProperty("spring.data.redis.host", redisContainer.getHost());
        System.setProperty("spring.data.redis.port", redisContainer.getMappedPort(6379).toString());
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
    void 세션_등록_시_직렬화에_실패하면_SerializationFailedException_예외가_발생한다() throws JsonProcessingException {
        // given
        JwtInformation session = createTestSession("access-token-1", "refresh-token-1");
        given(mockObjectMapper.writeValueAsString(any(JwtInformation.class)))
            .willThrow(JsonProcessingException.class);

        // when
        Throwable thrown = catchThrowable(() -> {
            jwtRegistry.register(session);
        });

        // then
        assertThat(thrown)
            .isInstanceOf(SerializationFailedException.class)
            .hasCauseInstanceOf(JsonProcessingException.class);
    }
}
