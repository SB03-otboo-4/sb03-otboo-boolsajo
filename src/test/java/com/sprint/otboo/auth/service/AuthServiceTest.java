package com.sprint.otboo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.nimbusds.jose.JOSEException;
import com.sprint.otboo.auth.dto.AuthResultDto;
import com.sprint.otboo.auth.dto.SignInRequest;
import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.common.exception.auth.AccountLockedException;
import com.sprint.otboo.common.exception.auth.InvalidCredentialsException;
import com.sprint.otboo.common.exception.auth.InvalidTokenException;
import com.sprint.otboo.common.exception.auth.TokenCreationException;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import java.text.ParseException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String TEST_EMAIL = "t1@abc.com";
    private static final String TEST_PASSWORD = "password1234";
    private static final String ENCODED_PASSWORD = "{bcrypt}encoded_password";
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    private CustomUserDetails createTestUserDetails(boolean isLocked) {
        UserDto userDto = new UserDto(
            TEST_USER_ID, Instant.now(),
            TEST_EMAIL, "t1", Role.USER, LoginType.GENERAL, isLocked
        );
        return new CustomUserDetails(userDto, ENCODED_PASSWORD);
    }

    @Test
    void 로그인_성공__AuthResultDto를_반환한다() throws Exception {
        // given
        SignInRequest request = new SignInRequest(TEST_EMAIL, TEST_PASSWORD);
        CustomUserDetails userDetails = createTestUserDetails(false);
        UserDto userDto = userDetails.getUserDto();

        given(userDetailsService.loadUserByUsername(TEST_EMAIL)).willReturn(userDetails);
        given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(tokenProvider.createAccessToken(userDto)).willReturn("access.jwt.token");
        given(tokenProvider.createRefreshToken(userDto)).willReturn("refresh.jwt.token");

        // when
        AuthResultDto result = authService.signIn(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access.jwt.token");
        assertThat(result.refreshToken()).isEqualTo("refresh.jwt.token");
        assertThat(result.userDto()).isEqualTo(userDto);

        verify(userDetailsService).loadUserByUsername(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(tokenProvider).createAccessToken(userDto);
        verify(tokenProvider).createRefreshToken(userDto);
    }

    @Test
    void 로그인_실패_존재하지않는_사용자() {
        // given
        String nonExistEmail = "no@abc.com";
        given(userDetailsService.loadUserByUsername(nonExistEmail))
            .willThrow(new UsernameNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + nonExistEmail));

        // when
        Throwable thrown = catchThrowable(() -> authService.signIn(new SignInRequest(nonExistEmail, "any_password")));

        // then
        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        verify(userDetailsService).loadUserByUsername(nonExistEmail);
        verifyNoInteractions(passwordEncoder, tokenProvider);
    }

    @Test
    void 로그인_실패_비밀번호_불일치() {
        // given
        CustomUserDetails userDetails = createTestUserDetails(false);
        String wrongPassword = "1324";

        given(userDetailsService.loadUserByUsername(TEST_EMAIL)).willReturn(userDetails);
        given(passwordEncoder.matches(wrongPassword, ENCODED_PASSWORD)).willReturn(false);

        // when
        Throwable thrown = catchThrowable(() -> authService.signIn(new SignInRequest(TEST_EMAIL, wrongPassword)));

        // then
        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        verify(userDetailsService).loadUserByUsername(TEST_EMAIL);
        verify(passwordEncoder).matches(wrongPassword, ENCODED_PASSWORD);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void 로그인_실패_잠긴계정() {
        // given
        CustomUserDetails userDetails = createTestUserDetails(true);
        given(userDetailsService.loadUserByUsername(TEST_EMAIL)).willReturn(userDetails);

        // when
        Throwable thrown = catchThrowable(() -> authService.signIn(new SignInRequest(TEST_EMAIL, TEST_PASSWORD)));

        // then
        assertThat(thrown).isInstanceOf(AccountLockedException.class);
        verify(userDetailsService).loadUserByUsername(TEST_EMAIL);
        verifyNoInteractions(passwordEncoder, tokenProvider);
    }

    @Test
    void 로그인_실패_토큰생성_오류() throws JOSEException {
        // given
        SignInRequest request = new SignInRequest(TEST_EMAIL, TEST_PASSWORD);
        CustomUserDetails userDetails = createTestUserDetails(false);
        UserDto userDto = userDetails.getUserDto();

        given(userDetailsService.loadUserByUsername(TEST_EMAIL)).willReturn(userDetails);
        given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);

        given(tokenProvider.createAccessToken(userDto))
            .willThrow(new JOSEException("Failed to sign JWT"));

        // when
        Throwable thrown = catchThrowable(() -> authService.signIn(request));

        // then
        assertThat(thrown).isInstanceOf(TokenCreationException.class);
        verify(userDetailsService).loadUserByUsername(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(tokenProvider).createAccessToken(userDto);
    }

    @Test
    void 토큰재발급_성공() throws Exception {
        // given
        String refreshToken = "valid.refresh.token";
        CustomUserDetails userDetails = createTestUserDetails(false);
        UserDto userDto = userDetails.getUserDto();
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";

        doNothing().when(tokenProvider).validateRefreshToken(refreshToken);
        given(tokenProvider.getEmailFromRefreshToken(refreshToken)).willReturn(TEST_EMAIL);
        given(userDetailsService.loadUserByUsername(TEST_EMAIL)).willReturn(userDetails);
        given(tokenProvider.createAccessToken(userDto)).willReturn(newAccessToken);
        given(tokenProvider.createRefreshToken(userDto)).willReturn(newRefreshToken);

        // when
        AuthResultDto result = authService.reissueToken(refreshToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);
        assertThat(result.userDto()).isEqualTo(userDto);

        verify(tokenProvider).validateRefreshToken(refreshToken);
        verify(tokenProvider).getEmailFromRefreshToken(refreshToken);
        verify(userDetailsService).loadUserByUsername(TEST_EMAIL);
        verify(tokenProvider).createAccessToken(userDto);
        verify(tokenProvider).createRefreshToken(userDto);
    }

    @Test
    void 토큰재발급_실패_유효하지않은_토큰() throws ParseException {
        // given
        String invalidRefreshToken = "invalid.refresh.token";

        doThrow(new InvalidTokenException())
            .when(tokenProvider).validateRefreshToken(invalidRefreshToken);

        // when
        Throwable thrown = catchThrowable(() -> authService.reissueToken(invalidRefreshToken));

        // then
        assertThat(thrown).isInstanceOf(InvalidTokenException.class);

        verify(tokenProvider).validateRefreshToken(invalidRefreshToken);
        verifyNoInteractions(userDetailsService, passwordEncoder);
    }

    @Test
    void 로그아웃_성공() throws ParseException {
        // given
        String validRefreshToken = "valid.refresh.token";
        doNothing().when(tokenProvider).validateRefreshToken(validRefreshToken);

        // when
        authService.signOut(validRefreshToken);

        // then
        verify(tokenProvider).validateRefreshToken(validRefreshToken);
    }

    @Test
    void 로그아웃_실패__유효하지_않은_토큰() throws ParseException {
        // given
        String invalidRefreshToken = "invalid.refresh.token";
        doThrow(new InvalidTokenException()).when(tokenProvider).validateRefreshToken(invalidRefreshToken);

        // when
        Throwable thrown = catchThrowable(() -> authService.signOut(invalidRefreshToken));

        // then
        assertThat(thrown)
            .isInstanceOf(InvalidTokenException.class);

        verify(tokenProvider).validateRefreshToken(invalidRefreshToken);
    }

}
