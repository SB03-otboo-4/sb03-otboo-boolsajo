package com.sprint.otboo.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.nimbusds.jose.JOSEException;
import com.sprint.otboo.common.exception.auth.InvalidTokenException;
import com.sprint.otboo.common.exception.auth.TokenExpiredException;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import java.text.ParseException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TokenProviderTest {

    @Autowired
    private TokenProvider tokenProvider;
    private UserDto testUserDto;

    @Value("${test-jwt.access-token.secret}")
    private String testAccessTokenSecret;
    @Value("${test-jwt.refresh-token.secret}")
    private String testRefreshTokenSecret;
    @Value("${test-jwt.access-token.expiration}")
    private long accessTokenExpiration;
    @Value("${test-jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    @BeforeEach
    void setUp() {
        testUserDto = new UserDto(
            UUID.randomUUID(),
            Instant.now(),
            "test@abc.com",
            "testuser",
            Role.USER,
            null,
            false
        );
    }

    @Test
    void 유효한_사용자_정보로_Access_Token을_생성한다() throws JOSEException, ParseException {
        // when
        String accessToken = tokenProvider.createAccessToken(testUserDto);

        // then
        assertNotNull(accessToken);
        Authentication authentication = tokenProvider.getAuthentication(accessToken);
        assertEquals(testUserDto.email(), authentication.getName());
    }

    @Test
    void 유효한_사용자_정보로_Refresh_Token을_생성한다() throws JOSEException, ParseException {
        // when
        String refreshToken = tokenProvider.createRefreshToken(testUserDto);

        // then
        assertNotNull(refreshToken);
        assertEquals(testUserDto.email(), tokenProvider.getEmailFromRefreshToken(refreshToken));
    }

    @Test
    void 유효한_Access_Token은_검증을_통과한다() throws JOSEException {
        // given
        String accessToken = tokenProvider.createAccessToken(testUserDto);

        // when
        Throwable thrown = catchThrowable(() -> tokenProvider.validateAccessToken(accessToken));

        // then
        assertThat(thrown).isNull();
    }

    @Test
    void 만료된_Access_Token은_TokenExpiredException을_던진다() throws JOSEException, InterruptedException {
        // given
        TokenProvider expiredTokenProvider = new TokenProvider(testAccessTokenSecret, 1, testRefreshTokenSecret, 1);
        String accessToken = expiredTokenProvider.createAccessToken(testUserDto);
        Thread.sleep(10);

        // when
        Throwable thrown = catchThrowable(() -> expiredTokenProvider.validateAccessToken(accessToken));

        // then
        assertThat(thrown).isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void 잘못된_서명을_가진_Access_Token은_InvalidTokenException을_던진다() throws JOSEException {
        // given
        String accessToken = tokenProvider.createAccessToken(testUserDto);
        String wrongSecretKey = "this-is-a-sufficiently-long-and-different-secret-key-for-testing";
        TokenProvider verifierWithWrongSecret = new TokenProvider(wrongSecretKey, accessTokenExpiration, testRefreshTokenSecret, refreshTokenExpiration);

        // when
        Throwable thrown = catchThrowable(() -> verifierWithWrongSecret.validateAccessToken(accessToken));

        // then
        assertThat(thrown).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void Refresh_Token을_Access_Token_검증_메서드로_검증하면_InvalidTokenException을_던진다() throws JOSEException {
        // given
        String refreshToken = tokenProvider.createRefreshToken(testUserDto);

        // when
        Throwable thrown = catchThrowable(() -> tokenProvider.validateAccessToken(refreshToken));

        // then
        assertThat(thrown).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void 형식이_잘못된_토큰은_ParseException을_던진다() {
        // given
        String incorrectToken = "this.is.not.a.jwt";

        // when
        Throwable thrown = catchThrowable(() -> tokenProvider.validateAccessToken(incorrectToken));

        // then
        assertThat(thrown).isInstanceOf(ParseException.class);
    }

    @Test
    void 유효한_Access_Token에서_Authentication_객체를_성공적으로_추출한다() throws JOSEException, ParseException {
        // given
        String accessToken = tokenProvider.createAccessToken(testUserDto);

        // when
        Authentication authentication = tokenProvider.getAuthentication(accessToken);

        // then
        assertNotNull(authentication);
        assertEquals(testUserDto.email(), authentication.getName());
    }

    @Test
    void 유효한_Refresh_Token에서_이메일을_성공적으로_추출한다() throws JOSEException, ParseException {
        // given
        String refreshToken = tokenProvider.createRefreshToken(testUserDto);

        // when
        String email = tokenProvider.getEmailFromRefreshToken(refreshToken);

        // then
        assertEquals(testUserDto.email(), email);
    }

}
