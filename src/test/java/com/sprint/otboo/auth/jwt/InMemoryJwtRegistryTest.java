package com.sprint.otboo.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.sprint.otboo.auth.dto.JwtInformation;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryJwtRegistryTest {

    private InMemoryJwtRegistry jwtRegistry;
    private UserDto user1Dto;
    private UserDto user2Dto;

    @BeforeEach
    void setUp() {
        int MAX_ACTIVE_SESSION_COUNT = 2;
        jwtRegistry = new InMemoryJwtRegistry(MAX_ACTIVE_SESSION_COUNT);

        user1Dto = new UserDto(UUID.randomUUID(), Instant.now(), "user1@test.com", "user1", Role.USER, null, false);
        user2Dto = new UserDto(UUID.randomUUID(), Instant.now(), "user2@test.com", "user2", Role.USER, null, false);
    }

    private JwtInformation createJwtInfo(UserDto userDto) {
        String randomAccessToken = "access-" + UUID.randomUUID();
        String randomRefreshToken = "refresh-" + UUID.randomUUID();
        return new JwtInformation(userDto, randomAccessToken, randomRefreshToken);
    }

    @Test
    void 새로운_세션을_성공적으로_등록한다() {
        // given
        JwtInformation session1 = createJwtInfo(user1Dto);

        // when
        jwtRegistry.register(session1);

        // then
        assertThat(jwtRegistry.isAccessTokenValid(session1.accessToken())).isTrue();
        assertThat(jwtRegistry.isRefreshTokenValid(session1.refreshToken())).isTrue();
    }

    @Test
    void 최대_세션_수를_초과하면_가장_오래된_세션이_자동으로_무효화된다() {
        // given
        JwtInformation session1 = createJwtInfo(user1Dto);
        JwtInformation session2 = createJwtInfo(user1Dto);
        jwtRegistry.register(session1);
        jwtRegistry.register(session2);

        // when
        JwtInformation session3 = createJwtInfo(user1Dto);
        jwtRegistry.register(session3);

        // then
        assertThat(jwtRegistry.isAccessTokenValid(session1.accessToken())).isFalse();
        assertThat(jwtRegistry.isRefreshTokenValid(session1.refreshToken())).isFalse();

        assertThat(jwtRegistry.isAccessTokenValid(session2.accessToken())).isTrue();
        assertThat(jwtRegistry.isAccessTokenValid(session3.accessToken())).isTrue();
    }

    @Test
    void 다른_사용자의_세션은_서로_영향을_주지_않는다() {
        // given
        JwtInformation user1Session = createJwtInfo(user1Dto);
        JwtInformation user2Session = createJwtInfo(user2Dto);

        // when
        jwtRegistry.register(user1Session);
        jwtRegistry.register(user2Session);

        // then
        assertThat(jwtRegistry.isAccessTokenValid(user1Session.accessToken())).isTrue();
        assertThat(jwtRegistry.isAccessTokenValid(user2Session.accessToken())).isTrue();
    }

    @Test
    void 특정_Refresh_Token으로_세션을_성공적으로_무효화한다() {
        // given
        JwtInformation session1 = createJwtInfo(user1Dto);
        JwtInformation session2 = createJwtInfo(user1Dto);
        jwtRegistry.register(session1);
        jwtRegistry.register(session2);

        // when
        jwtRegistry.invalidate(session1.refreshToken());

        // then
        assertThat(jwtRegistry.isAccessTokenValid(session1.accessToken())).isFalse();
        assertThat(jwtRegistry.isRefreshTokenValid(session1.refreshToken())).isFalse();
        assertThat(jwtRegistry.isAccessTokenValid(session2.accessToken())).isTrue();
    }

    @Test
    void 존재하지_않는_Refresh_Token으로_무효화_시도_시_아무_일도_일어나지_않는다() {
        // given
        JwtInformation session1 = createJwtInfo(user1Dto);
        jwtRegistry.register(session1);

        // when
        Throwable thrown = catchThrowable(() -> jwtRegistry.invalidate("non-existent-token"));

        // then
        assertThat(thrown).isNull();
        assertThat(jwtRegistry.isAccessTokenValid(session1.accessToken())).isTrue();
    }

    @Test
    void 특정_사용자의_모든_세션을_성공적으로_무효화한다() {
        // given
        JwtInformation user1Session1 = createJwtInfo(user1Dto);
        JwtInformation user1Session2 = createJwtInfo(user1Dto);
        JwtInformation user2Session1 = createJwtInfo(user2Dto);
        jwtRegistry.register(user1Session1);
        jwtRegistry.register(user1Session2);
        jwtRegistry.register(user2Session1);

        // when
        jwtRegistry.invalidateAll(user1Dto.id());

        // then
        assertThat(jwtRegistry.isAccessTokenValid(user1Session1.accessToken())).isFalse();
        assertThat(jwtRegistry.isAccessTokenValid(user1Session2.accessToken())).isFalse();
        assertThat(jwtRegistry.isAccessTokenValid(user2Session1.accessToken())).isTrue();
    }
}
