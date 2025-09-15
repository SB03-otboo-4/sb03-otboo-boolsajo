package com.sprint.otboo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.user.service.impl.UserServiceImpl;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void 새로운_사용자_등록_성공() {
        // given
        UserCreateRequest request = new UserCreateRequest(
            "testUser",
            "test@test.com",
            "test1234"
        );

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByUsername(request.name())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        User savedUser = User.builder()
            .username(request.name())
            .password("encodedPassword")
            .email(request.email())
            .role(Role.USER)
            .provider(LoginType.GENERAL)
            .locked(false)
            .build();

        UserDto expectedUserDto = new UserDto(
            userId,
            now,
            request.email(),
            request.name(),
            Role.USER,
            LoginType.GENERAL,
            false
        );

        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(userMapper.toUserDto(savedUser)).willReturn(expectedUserDto);

        // when
        UserDto result = userService.createUser(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo(request.email());
        assertThat(result.name()).isEqualTo(request.name());
        assertThat(result.role()).isEqualTo(Role.USER);
        assertThat(result.linkedOAuthProviders()).isEqualTo(LoginType.GENERAL);
        assertThat(result.locked()).isFalse();

        then(userRepository).should().existsByEmail(request.email());
        then(userRepository).should().existsByUsername(request.name());
        then(passwordEncoder).should().encode(request.password());
        then(userRepository).should().save(any(User.class));
        then(userMapper).should().toUserDto(savedUser);
    }

    @Test
    void 중복된_이메일로_사용자_등록시_예외_발생() {
        // given
        UserCreateRequest request = new UserCreateRequest(
            "testUser",
            "test@test.com",
            "test1234"
        );

        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when
        Throwable thrown = catchThrowable(() -> userService.createUser(request));

        // then
        assertThat(thrown)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 사용 중인 이메일입니다 : " + request.email());

        then(userRepository).should().existsByEmail(request.email());
        then(userRepository).should(never()).existsByUsername(anyString());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    void 중복된_사용자명으로_사용자_등록시_예외_발생() {
        // given
        UserCreateRequest request = new UserCreateRequest(
            "testUser",
            "test@test.com",
            "test1234"
        );

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByUsername(request.name())).willReturn(true);

        // when
        Throwable thrown = catchThrowable(() -> userService.createUser(request));

        // then
        assertThat(thrown)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 사용 중인 이름입니다 : " + request.name());

        then(userRepository).should().existsByEmail(request.email());
        then(userRepository).should().existsByUsername(request.name());
        then(userRepository).should(never()).save(any(User.class));
    }
}
