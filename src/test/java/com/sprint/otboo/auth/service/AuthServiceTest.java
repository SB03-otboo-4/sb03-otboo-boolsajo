package com.sprint.otboo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.nimbusds.jose.JOSEException;
import com.sprint.otboo.auth.dto.JwtDto;
import com.sprint.otboo.auth.dto.SignInRequest;
import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.common.exception.auth.AccountLockedException;
import com.sprint.otboo.common.exception.auth.InvalidCredentialsException;
import com.sprint.otboo.common.exception.auth.TokenCreationException;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
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
    void 로그인_성공_JwtDto를_반환한다() throws Exception {
        // given
        SignInRequest request = new SignInRequest(TEST_EMAIL, TEST_PASSWORD);
        CustomUserDetails userDetails = createTestUserDetails(false);
        UserDto userDto = userDetails.getUserDto();

        given(userDetailsService.loadUserByUsername(TEST_EMAIL)).willReturn(userDetails);
        given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(tokenProvider.createAccessToken(userDto)).willReturn("access.jwt.token");

        // when
        JwtDto result = authService.signIn(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access.jwt.token");
        assertThat(result.userDto()).isEqualTo(userDto);

        verify(userDetailsService).loadUserByUsername(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(tokenProvider).createAccessToken(userDto);
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
}
