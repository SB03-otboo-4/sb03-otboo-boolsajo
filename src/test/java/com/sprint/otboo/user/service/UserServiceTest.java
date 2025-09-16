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
import com.sprint.otboo.user.dto.request.UserLockUpdateRequest;
import com.sprint.otboo.user.dto.request.UserRoleUpdateRequest;
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

    private ChangePasswordRequest createPasswordChangeRequest(String password) {
        return new ChangePasswordRequest(password);
    }

    private User createMockUserForPasswordChange(UUID userId, String encodedPassword) {
        return User.builder()
            .id(userId)
            .username("testUser")
            .email("test@test.com")
            .password(encodedPassword)
            .role(Role.USER)
            .locked(false)
            .build();
    }

    private void setupUserNotFound(UUID userId) {
        given(userRepository.findById(userId)).willReturn(Optional.empty());
    }

    private UserLockUpdateRequest createLockUpdateRequest(boolean locked) {
        return new UserLockUpdateRequest(locked);
    }

    private User createMockUserForLockUpdate(UUID userId, boolean currentLockStatus) {
        return User.builder()
            .id(userId)
            .username("testUser")
            .email("test@test.com")
            .password("encodedPassword")
            .role(Role.USER)
            .locked(currentLockStatus)
            .provider(LoginType.GENERAL)
            .build();
    }

    private UserDto createExpectedUserDtoForLockUpdate(UUID userId, boolean locked) {
        return new UserDto(
            userId,
            Instant.now(),
            "test@test.com",
            "testUser",
            Role.USER,
            LoginType.GENERAL,
            locked
        );
    }

    private UserRoleUpdateRequest createRoleUpdateRequest(String role) {
        return new UserRoleUpdateRequest(role);
    }

    private User createMockUserForRoleUpdate(UUID userId, Role currentRole) {
        return User.builder()
            .id(userId)
            .username("testUser")
            .email("test@test.com")
            .password("encodedPassword")
            .role(currentRole)
            .locked(false)
            .provider(LoginType.GENERAL)
            .build();
    }

    private UserDto createExpectedUserDtoForRoleUpdate(UUID userId, Role role) {
        return new UserDto(
            userId,
            Instant.now(),
            "test@test.com",
            "testUser",
            role,
            LoginType.GENERAL,
            false
        );
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
    void 기존_비밀번호와_동일한_비밀번호로_변경시_예외_발생() {
        // given
        UUID userId = UUID.randomUUID();
        String currentPassword = "";
        String encodedCurrentPassword = "samePassWord123";

        User existingUser = createMockUserForPasswordChange(userId, encodedCurrentPassword);
        ChangePasswordRequest request = createPasswordChangeRequest(currentPassword);

        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));
        given(passwordEncoder.matches(currentPassword, encodedCurrentPassword)).willReturn(true);

        // when
        Throwable thrown = catchThrowable(() -> userService.updatePassword(userId, request));

        // then
        assertThat(thrown)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.SAME_PASSWORD);

        then(userRepository).should().findById(userId);
        then(passwordEncoder).should().matches(currentPassword, encodedCurrentPassword);
        then(passwordEncoder).should(never()).encode(anyString());
    }

    @Test
    void 비밀번호_변경_성공() {
        // given
        UUID userId = UUID.randomUUID();
        String newPassword = "newPassword1234";
        String encodedCurrentPassword = "encodedOldPassword";
        String encodedNewPassword = "encodedNewPassword1234";

        User existingUser = createMockUserForPasswordChange(userId, encodedCurrentPassword);
        ChangePasswordRequest request = createPasswordChangeRequest(newPassword);

        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));
        given(passwordEncoder.matches(newPassword, encodedCurrentPassword)).willReturn(false);
        given(passwordEncoder.encode(newPassword)).willReturn(encodedNewPassword);

        // when
        userService.updatePassword(userId, request);

        // then
        then(userRepository).should().findById(userId);
        then(passwordEncoder).should().matches(newPassword, encodedCurrentPassword);
        then(passwordEncoder).should().encode(newPassword);

        assertThat(existingUser.getPassword()).isEqualTo(encodedNewPassword);
    }

    @Test
    void 존재하지_않는_사용자_비밀번호_변경시_예외_발생() {
        // given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = createPasswordChangeRequest("newPassword123");
        setupUserNotFound(userId);

        // when
        Throwable thrown = catchThrowable(() -> userService.updatePassword(userId, request));

        // then
        assertThat(thrown)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.USER_NOT_FOUND);

        then(userRepository).should().findById(userId);
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
        then(passwordEncoder).should(never()).encode(anyString());
    }

    @Test
    void 계정_잠금_상태_변경_성공() {
        // given
        UUID userId = UUID.randomUUID();
        UserLockUpdateRequest request = createLockUpdateRequest(true);
        User mockUser = createMockUserForLockUpdate(userId, false);
        UserDto expectedDto = createExpectedUserDtoForLockUpdate(userId, true);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(userRepository.save(any(User.class))).willReturn(mockUser);
        given(userMapper.toUserDto(mockUser)).willReturn(expectedDto);

        // when
        UserDto result = userService.updateUserLockStatus(userId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.locked()).isTrue();

        then(userRepository).should().findById(userId);
        then(userRepository).should().save(any(User.class));
        then(userMapper).should().toUserDto(any(User.class));
    }

    @Test
    void 계정_잠금_해제_성공() {
        // given
        UUID userId = UUID.randomUUID();
        UserLockUpdateRequest request = createLockUpdateRequest(false);
        User mockUser = createMockUserForLockUpdate(userId, true);
        UserDto expectedDto = createExpectedUserDtoForLockUpdate(userId, false);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(userRepository.save(any(User.class))).willReturn(mockUser);
        given(userMapper.toUserDto(mockUser)).willReturn(expectedDto);


        // when
        UserDto result = userService.updateUserLockStatus(userId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.locked()).isFalse();

        then(userRepository).should().findById(userId);
        then(userRepository).should().save(any(User.class));
        then(userMapper).should().toUserDto(mockUser);
    }

    @Test
    void 계정_잠금_상태_변경_실패_사용자_없음() {
        // given
        UUID userId = UUID.randomUUID();
        UserLockUpdateRequest request = createLockUpdateRequest(true);
        setupUserNotFound(userId);

        // when
        Throwable thrown = catchThrowable(() -> userService.updateUserLockStatus(userId, request));

        // then
        assertThat(thrown)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.USER_NOT_FOUND);

        then(userRepository).should().findById(userId);
        then(userRepository).should(never()).save(any(User.class));
        then(userMapper).should(never()).toUserDto(any(User.class));
    }

    @Test
    void 권한_수정_USER에서_ADMIN으로_성공() {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = createRoleUpdateRequest("ADMIN");
        User mockUser = createMockUserForRoleUpdate(userId, Role.USER);
        UserDto expectedDto = createExpectedUserDtoForRoleUpdate(userId, Role.ADMIN);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(userRepository.save(any(User.class))).willReturn(mockUser);
        given(userMapper.toUserDto(mockUser)).willReturn(expectedDto);

        // when
        UserDto result = userService.updateUserRole(userId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.role()).isEqualTo(Role.ADMIN);

        then(userRepository).should().findById(userId);
        then(userRepository).should().save(any(User.class));
        then(userMapper).should().toUserDto(mockUser);
    }

    @Test
    void 권한_수정_ADMIN에서_USER로_성공() {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = createRoleUpdateRequest("USER");
        User mockUser = createMockUserForRoleUpdate(userId, Role.ADMIN);
        UserDto expectedDto = createExpectedUserDtoForRoleUpdate(userId, Role.USER);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(userRepository.save(any(User.class))).willReturn(mockUser);
        given(userMapper.toUserDto(mockUser)).willReturn(expectedDto);

        // when
        UserDto result = userService.updateUserRole(userId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.role()).isEqualTo(Role.USER);

        then(userRepository).should().findById(userId);
        then(userRepository).should().save(any(User.class));
        then(userMapper).should().toUserDto(mockUser);
    }

    @Test
    void 권한_수정_실패_사용자_없음() {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = createRoleUpdateRequest("ADMIN");
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> userService.updateUserRole(userId, request));

        // then
        assertThat(thrown)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.USER_NOT_FOUND);

        then(userRepository).should().findById(userId);
        then(userRepository).should(never()).save(any(User.class));
        then(userMapper).should(never()).toUserDto(any(User.class));
    }

    @Test
    void 동일한_권한으로_수정시_그대로_반환() {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = createRoleUpdateRequest("USER");
        User mockUser = createMockUserForRoleUpdate(userId, Role.USER);
        UserDto expectedDto = createExpectedUserDtoForRoleUpdate(userId, Role.USER);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(userRepository.save(any(User.class))).willReturn(mockUser);
        given(userMapper.toUserDto(mockUser)).willReturn(expectedDto);

        // when
        UserDto result = userService.updateUserRole(userId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.role()).isEqualTo(Role.USER);

        then(userRepository).should().findById(userId);
        then(userRepository).should().save(any(User.class));
    }
}