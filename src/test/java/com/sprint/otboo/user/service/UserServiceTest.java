package com.sprint.otboo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.ChangePasswordRequest;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.user.service.impl.UserServiceImpl;
import java.time.Instant;
import java.util.Optional;
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

    // 팩토리 메서드들
    private UserCreateRequest createDefaultRequest() {
        return new UserCreateRequest(
            "testUser",
            "test@test.com",
            "test1234"
        );
    }

    private UserCreateRequest createRequestWithEmail(String email) {
        return new UserCreateRequest(
            "testUser",
            email,
            "test1234"
        );
    }

    private UserCreateRequest createRequestWithName(String name) {
        return new UserCreateRequest(
            name,
            "test@test.com",
            "test1234"
        );
    }

    private User createMockUser(UserCreateRequest request) {
        return User.builder()
            .username(request.name())
            .password("encodedPassword")
            .email(request.email())
            .role(Role.USER)
            .provider(LoginType.GENERAL)
            .locked(false)
            .build();
    }

    private UserDto createExpectedUserDto(UserCreateRequest request) {
        return new UserDto(
            UUID.randomUUID(),
            Instant.now(),
            request.email(),
            request.name(),
            Role.USER,
            LoginType.GENERAL,
            false
        );
    }

    private void setupSuccessfulUserCreation(UserCreateRequest request) {
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByUsername(request.name())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

        User savedUser = createMockUser(request);
        UserDto expectedDto = createExpectedUserDto(request);

        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(userMapper.toUserDto(savedUser)).willReturn(expectedDto);
    }

    @Test
    void 새로운_사용자_등록_성공() {
        // given
        UserCreateRequest request = createDefaultRequest();
        setupSuccessfulUserCreation(request);

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
        then(userMapper).should().toUserDto(any(User.class));
    }

    @Test
    void 중복된_이메일로_사용자_등록시_예외_발생() {
        // given
        UserCreateRequest request = createDefaultRequest();
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when
        Throwable thrown = catchThrowable(() -> userService.createUser(request));

        // then
        assertThat(thrown)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        CustomException customException = (CustomException) thrown;
        assertThat(customException.getDetails())
            .containsEntry("email", request.email());

        then(userRepository).should().existsByEmail(request.email());
        then(userRepository).should(never()).existsByUsername(anyString());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    void 중복된_사용자명으로_사용자_등록시_예외_발생() {
        // given
        UserCreateRequest request = createDefaultRequest();
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByUsername(request.name())).willReturn(true);

        // when
        Throwable thrown = catchThrowable(() -> userService.createUser(request));

        // then
        assertThat(thrown)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_USERNAME);

        CustomException customException = (CustomException) thrown;
        assertThat(customException.getDetails())
            .containsEntry("username", request.name());

        then(userRepository).should().existsByEmail(request.email());
        then(userRepository).should().existsByUsername(request.name());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    void 올바른_현재_비밀번호로_비밀번호_변경_성공() {
        // given
        UUID userId = UUID.randomUUID();
        String currentPassword = "test1234";
        String encodedCurrentPassword = "encodedCurrentPassword1234";
        String newPassword = "test5678";
        String encodedNewPassword = "encodedCurrentPassword5678";

        User existingUser = User.builder()
            .id(userId)
            .username("testUser")
            .email("test@test.com")
            .password(encodedCurrentPassword)
            .role(Role.USER)
            .locked(false)
            .build();

        ChangePasswordRequest request = new ChangePasswordRequest(
            currentPassword,
            newPassword
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));
        given(passwordEncoder.matches(currentPassword, encodedCurrentPassword)).willReturn(true);
        given(passwordEncoder.encode(newPassword)).willReturn(encodedNewPassword);

        // when
        userService.updatePassword(userId, request);

        // then
        then(userRepository).should().findById(userId);
        then(passwordEncoder).should().matches(currentPassword, encodedCurrentPassword);
        then(passwordEncoder).should().encode(newPassword);

        assertThat(existingUser.getPassword()).isEqualTo(encodedNewPassword);
    }

    @Test
    void 잘못된_현재_비밀번호로_비밀번호_변경시_예외_발생() {

        // given
        UUID userId = UUID.randomUUID();
        String currentPassword = "wrongPassword";
        String encodedCurrentPassword = "encodedCurrentPassword1234";
        String newPassword = "test5678";

        User existingUser = User.builder()
            .id(userId)
            .username("testUser")
            .email("test@test.com")
            .password(encodedCurrentPassword)
            .role(Role.USER)
            .locked(false)
            .build();


        ChangePasswordRequest request = new ChangePasswordRequest(
            currentPassword,
            newPassword
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));
        given(passwordEncoder.matches(currentPassword, encodedCurrentPassword)).willReturn(false);

        // when
        Throwable thrown = catchThrowable(() -> userService.updatePassword(userId, request));

        // then
        assertThat(thrown)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("현재 비밀번호가 올바르지 않습니다");

        then(userRepository).should().findById(userId);
        then(passwordEncoder).should().matches(currentPassword, encodedCurrentPassword);
        then(passwordEncoder).should(never()).encode(anyString());
    }

    @Test
    void 존재하지_않는_사용자_비밀번호_변경시_예외_발생() {
        // given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest(
            "test1234",
            "test5678"
        );

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> userService.updatePassword(userId, request));

        // then
        assertThat(thrown)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("사용자를 찾을 수 없습니다");

        then(userRepository).should().findById(userId);
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
        then(passwordEncoder).should(never()).encode(anyString());
    }
}