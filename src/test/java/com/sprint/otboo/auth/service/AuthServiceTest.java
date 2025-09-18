package com.sprint.otboo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.otboo.auth.dto.JwtDto;
import com.sprint.otboo.auth.dto.SignInRequest;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.common.exception.auth.AccountLockedException;
import com.sprint.otboo.common.exception.auth.InvalidCredentialsException;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String TEST_EMAIL = "t1@abc.com";
    private static final String TEST_PASSWORD = "password1234";
    private static final String ENCODED_PASSWORD = "{bcrypt}encoded_password";
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    private User createTestUser(boolean isLocked) {
        return User.builder()
            .id(TEST_USER_ID)
            .username("t1")
            .email(TEST_EMAIL)
            .password(ENCODED_PASSWORD)
            .role(Role.USER)
            .locked(isLocked)
            .build();
    }

    @Test
    void 로그인_성공_JwtDto를_반환한다() throws Exception {
        // given
        User unlockedUser = createTestUser(false);

        UserDto userDto = new UserDto(
            TEST_USER_ID, Instant.now(),
            TEST_EMAIL, "t1", Role.USER, LoginType.GENERAL, false
        );
        SignInRequest request = new SignInRequest(TEST_EMAIL, TEST_PASSWORD);


        given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(unlockedUser));
        given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(tokenProvider.createAccessToken(unlockedUser)).willReturn("access.jwt.token");
        given(userMapper.toUserDto(unlockedUser)).willReturn(userDto);

        // when
        JwtDto result = authService.signIn(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access.jwt.token");
        assertThat(result.userDto()).isEqualTo(userDto);

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(tokenProvider).createAccessToken(unlockedUser);
        verify(userMapper).toUserDto(unlockedUser);
    }

    @Test
    void 로그인_실패_존재하지않는_사용자_401Error() {
        // given
        String nonExistEmail = "no@abc.com";
        given(userRepository.findByEmail(nonExistEmail)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.signIn(new SignInRequest(nonExistEmail, "any_password")))
            .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).findByEmail(nonExistEmail);
        verifyNoInteractions(passwordEncoder, tokenProvider, userMapper);
    }

    @Test
    void 로그인_실패_비밀번호_불일치_401Error() {
        // given
        User unlockedUser = createTestUser(false);
        String wrongPassword = "1324";

        given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(unlockedUser));
        given(passwordEncoder.matches(wrongPassword, ENCODED_PASSWORD)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.signIn(new SignInRequest(TEST_EMAIL, wrongPassword)))
            .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(wrongPassword, ENCODED_PASSWORD);
        verifyNoInteractions(tokenProvider, userMapper);
    }

    @Test
    void 로그인_실패_잠긴계정() {
        // given
        User lockedUser = createTestUser(true);
        given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(lockedUser));

        // when & then
        assertThatThrownBy(() -> authService.signIn(new SignInRequest(TEST_EMAIL, "1324")))
            .isInstanceOf(AccountLockedException.class);

        verify(userRepository).findByEmail(TEST_EMAIL);
        verifyNoInteractions(passwordEncoder, tokenProvider, userMapper);
    }
}
