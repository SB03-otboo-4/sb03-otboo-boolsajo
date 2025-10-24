package com.sprint.otboo.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.user.dto.data.ProfileDto;
import com.sprint.otboo.user.dto.data.ProfileLocationDto;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.ChangePasswordRequest;
import com.sprint.otboo.user.dto.request.ProfileLocationUpdateRequest;
import com.sprint.otboo.user.dto.request.ProfileUpdateRequest;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
import com.sprint.otboo.user.dto.request.UserListQueryParams;
import com.sprint.otboo.user.dto.request.UserLockUpdateRequest;
import com.sprint.otboo.user.dto.request.UserRoleUpdateRequest;
import com.sprint.otboo.user.entity.Gender;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.service.UserService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.MethodArgumentNotValidException;

@WebMvcTest(UserController.class)
@DisplayName("UserController 테스트")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    JwtRegistry jwtRegistry;

    @MockitoBean
    private UserService userService;

    private UserCreateRequest createDefaultRequest() {
        return new UserCreateRequest(
            "testUser",
            "test@test.com",
            "test1234"
        );
    }

    private UserCreateRequest createInvalidRequest(String name, String email, String password) {
        return new UserCreateRequest(name, email, password);
    }

    private UserDto createMockUserDto(UserCreateRequest request) {
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

    private ResultActions performCreateUserRequest(UserCreateRequest request) throws Exception {
        return mockMvc.perform(post("/api/users")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }

    private ChangePasswordRequest createPasswordChangeRequest(String password) {
        return new ChangePasswordRequest(password);
    }

    private ResultActions performUpdatePasswordRequest(UUID userId, ChangePasswordRequest request) throws Exception {
        return mockMvc.perform(patch("/api/users/{userId}/password", userId)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }

    private UserLockUpdateRequest createLockUpdateRequest(boolean locked) {
        return new UserLockUpdateRequest(locked);
    }

    private ResultActions performUpdateUserLockRequest(UUID userId, UserLockUpdateRequest request) throws Exception {
        return mockMvc.perform(patch("/api/users/{userId}/lock",userId)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }

    private UserRoleUpdateRequest createRoleUpdateRequest(String role) {
        return new UserRoleUpdateRequest(role);
    }

    private ResultActions performUpdateUserRoleRequest(UUID userId, UserRoleUpdateRequest request) throws Exception {
        return mockMvc.perform(patch("/api/users/{userId}/role", userId)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }

    private ProfileDto createCompleteProfileDto(UUID userId) {
        return new ProfileDto(
            userId,
            "testUser",
            "http://example.com/profile.jpg",
            Gender.MALE,
            LocalDate.of(1998, 9, 21),
            new ProfileLocationDto(
                new BigDecimal("37.509278"),
                new BigDecimal("126.671607"),
                55,
                125,
                List.of("인천광역시", "서구", "가정1동")
            ),
            5
        );
    }

    private ProfileDto createEmptyProfileDto(UUID userId) {
        return new ProfileDto(
            userId,
            "testUser",
            null,
            null,
            null,
            null,
            null
        );
    }

    private ResultActions performGetUserProfileRequest(UUID userId) throws Exception {
        return mockMvc.perform(get("/api/users/{userId}/profiles",userId)
            .contentType(MediaType.APPLICATION_JSON));
    }

    private UserDto createUserAccountDto(String email, String name, Role role, boolean locked) {
        return new UserDto(
            UUID.randomUUID(),
            Instant.now(),
            email,
            name,
            role,
            LoginType.GENERAL,
            locked
        );
    }

    private MockMultipartFile createProfileUpdatePart() throws Exception {
        ProfileUpdateRequest request = new ProfileUpdateRequest(
            "updatedName",
            "FEMALE",
            LocalDate.of(1998, 9, 21),
            new ProfileLocationUpdateRequest(
                new BigDecimal("37.5253652"),
                new BigDecimal("126.6849254"),
                55,
                126,
                List.of("인천광역시", "서구", "가정2동")
            ),
            5
        );

        return new MockMultipartFile(
            "request",
            "",
            "application/json",
            objectMapper.writeValueAsBytes(request)
        );
    }

    private RequestPostProcessor authenticatedUser(UUID userId, Role role) {
        UserDto dto = new UserDto(
            userId,
            Instant.now(),
            "tester@example.com",
            "tester",
            role,
            LoginType.GENERAL,
            false
        );
        CustomUserDetails principal = CustomUserDetails.builder()
            .userDto(dto)
            .password("password")
            .build();
        return user(principal);
    }

    @Test
    @WithMockUser
    void 사용자_등록에_성공() throws Exception {
        // given
        UserCreateRequest request = createDefaultRequest();
        UserDto responseDto = createMockUserDto(request);
        given(userService.createUser(any(UserCreateRequest.class))).willReturn(responseDto);

        // when
        ResultActions result = performCreateUserRequest(request);

        // then
        result.andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.email").value(request.email()))
            .andExpect(jsonPath("$.name").value(request.name()))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.linkedOAuthProviders").value("GENERAL"))
            .andExpect(jsonPath("$.locked").value(false))
            .andExpect(jsonPath("$.createdAt").exists());

        then(userService).should().createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser
    void 빈_이름으로_사용자_등록_시_400_에러_발생() throws Exception {
        // given
        UserCreateRequest request = createInvalidRequest("", "test@test.com", "test1234");

        // when
        ResultActions result = performCreateUserRequest(request);

        // then
        result.andExpect(status().isBadRequest());
        then(userService).should(never()).createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser
    void 중복된_이메일로_사용자_등록_시_409_에러_발생() throws Exception {
        // given
        UserCreateRequest request = createDefaultRequest();
        CustomException exception = new CustomException(ErrorCode.DUPLICATE_EMAIL);
        exception.addDetail("email", request.email());

        given(userService.createUser(any(UserCreateRequest.class))).willThrow(exception);

        // when
        ResultActions result = performCreateUserRequest(request);

        // then
        result.andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
            .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."))
            .andExpect(jsonPath("$.details.email").value(request.email()));

        then(userService).should().createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser
    void 비밀번호_변경에_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = createPasswordChangeRequest("newPassword1234");

        // when
        ResultActions result = performUpdatePasswordRequest(userId, request);

        // then
        result.andExpect(status().isNoContent());
        then(userService).should().updatePassword(userId, request);
    }

    @Test
    @WithMockUser
    void 존재하지_않는_사용자_비밀번호_변경시_404_에러_발생() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = createPasswordChangeRequest("newPassword1234");
        CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND);

        doThrow(exception).when(userService).updatePassword(userId, request);

        // when
        ResultActions result = performUpdatePasswordRequest(userId, request);

        // then
        result.andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        then(userService).should().updatePassword(userId, request);
    }

    @Test
    @WithMockUser
    void 유효하지_않은_비밀번호_형식으로_변경시_400_에러_발생() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = createPasswordChangeRequest("test123");

        // when
        ResultActions result = performUpdatePasswordRequest(userId, request);

        // then
        result.andExpect(status().isBadRequest());
        then(userService).should(never()).updatePassword(any(UUID.class), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser
    void 기존_비밀번호와_동일한_비밀번호로_변경시_400_에러_발생() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = createPasswordChangeRequest("test1234");
        CustomException exception = new CustomException(ErrorCode.SAME_PASSWORD);

        doThrow(exception).when(userService).updatePassword(userId, request);

        // when
        ResultActions result = performUpdatePasswordRequest(userId, request);

        // then
        result.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("SAME_PASSWORD"))
            .andExpect(jsonPath("$.message").value("새로운 비밀번호가 기존 비밀번호와 동일합니다."));

        then(userService).should().updatePassword(userId, request);
    }

    @Test
    @WithMockUser
    void 계정_잠금_상태_변경_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserLockUpdateRequest request = createLockUpdateRequest(true);
        UserDto mockUserDto = new UserDto(
            userId,
            Instant.now(),
            "test@test.com",
            "testUser",
            Role.USER,
            LoginType.GENERAL,
            true
        );
        given(userService.updateUserLockStatus(userId, request)).willReturn(mockUserDto);

        // when
        ResultActions result = performUpdateUserLockRequest(userId, request);

        // then
        result.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(userId.toString()))
            .andExpect(jsonPath("$.email").value("test@test.com"))
            .andExpect(jsonPath("$.name").value("testUser"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.linkedOAuthProviders").value("GENERAL"))
            .andExpect(jsonPath("$.locked").value(true));

        then(userService).should().updateUserLockStatus(userId, request);
    }

    @Test
    @WithMockUser
    void 계정_잠금_상태_변경_실패_사용자_없음() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserLockUpdateRequest request = createLockUpdateRequest(false);
        CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND);

        doThrow(exception).when(userService).updateUserLockStatus(userId, request);

        // when
        ResultActions result = performUpdateUserLockRequest(userId, request);

        // then
        result.andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        then(userService).should().updateUserLockStatus(userId, request);
    }

    @Test
    @WithMockUser
    void 잠금_상태가_null로_인해_400_에러_발생() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        String requestBody = "{\"locked\":null}";

        // when
        ResultActions result = mockMvc.perform(patch("/api/users/{userId}/lock", userId)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody));

        // then
        result.andExpect(status().isBadRequest());
        then(userService).should(never()).updateUserLockStatus(any(UUID.class),any(UserLockUpdateRequest.class));
    }

    @Test
    @WithMockUser
    void 권한_수정에_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = createRoleUpdateRequest("ADMIN");
        UserDto mockUserDto = new UserDto(
            userId,
            Instant.now(),
            "test@test.com",
            "testUser",
            Role.ADMIN,
            LoginType.GENERAL,
            false
        );
        given(userService.updateUserRole(userId, request)).willReturn(mockUserDto);

        // when
        ResultActions result = performUpdateUserRoleRequest(userId, request);

        // then
        result.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(userId.toString()))
            .andExpect(jsonPath("$.email").value("test@test.com"))
            .andExpect(jsonPath("$.name").value("testUser"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.locked").value(false))
            .andExpect(jsonPath("$.createdAt").exists());

        then(userService).should().updateUserRole(userId, request);
    }

    @Test
    @WithMockUser
    void 권한_수정_실패_사용자_없음() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = createRoleUpdateRequest("ADMIN");
        CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND);

        doThrow(exception).when(userService).updateUserRole(userId, request);

        // when
        ResultActions result = performUpdateUserRoleRequest(userId, request);

        // then
        result.andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        then(userService).should().updateUserRole(userId, request);
    }

    @Test
    @WithMockUser
    void 권한_수정_실패_유효하지_않은_권한() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = createRoleUpdateRequest("INVALID_ROLE");

        // when
        ResultActions result = performUpdateUserRoleRequest(userId, request);

        // then
        result.andExpect(status().isBadRequest());
        then(userService).should(never()).updateUserRole(any(UUID.class), any(UserRoleUpdateRequest.class));
    }

    @Test
    @WithMockUser
    void 권한_수정_실패_빈_권한() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        String requestBody = "{\"role\":\"\"}";

        // when
        ResultActions result = mockMvc.perform(patch("/api/users/{userId}/role", userId)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody));

        // then
        result.andExpect(status().isBadRequest());
        then(userService).should(never()).updateUserRole(any(UUID.class), any(UserRoleUpdateRequest.class));
    }

    @Test
    @WithMockUser
    void 권한_수정_실패_null_권한() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        String requestBody = "{\"role\":null}";

        // when
        ResultActions result = mockMvc.perform(patch("/api/users/{userId}/role", userId)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody));

        // then
        result.andExpect(status().isBadRequest());
        then(userService).should(never()).updateUserRole(any(UUID.class), any(UserRoleUpdateRequest.class));
    }

    @Test
    @WithMockUser
    void 완전한_프로필_정보_조회_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        ProfileDto profileDto = createCompleteProfileDto(userId);
        given(userService.getUserProfile(userId)).willReturn(profileDto);

        // when
        ResultActions result = performGetUserProfileRequest(userId);

        // then
        result.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.name").value("testUser"))
            .andExpect(jsonPath("$.profileImageUrl").value("http://example.com/profile.jpg"))
            .andExpect(jsonPath("$.gender").value("MALE"))
            .andExpect(jsonPath("$.birthDate").value("1998-09-21"))
            .andExpect(jsonPath("$.location.latitude").value(37.509278))
            .andExpect(jsonPath("$.location.longitude").value(126.671607))
            .andExpect(jsonPath("$.location.x").value(55))
            .andExpect(jsonPath("$.location.y").value(125))
            .andExpect(jsonPath("$.location.locationNames[0]").value("인천광역시"))
            .andExpect(jsonPath("$.location.locationNames[1]").value("서구"))
            .andExpect(jsonPath("$.location.locationNames[2]").value("가정1동"))
            .andExpect(jsonPath("$.temperatureSensitivity").value(5));

        then(userService).should().getUserProfile(userId);
    }

    @Test
    @WithMockUser
    void 빈_프로필_정보_조회_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        ProfileDto profileDto = createEmptyProfileDto(userId);
        given(userService.getUserProfile(userId)).willReturn(profileDto);

        // when
        ResultActions result = performGetUserProfileRequest(userId);

        // then
        result.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.name").value("testUser"))
            .andExpect(jsonPath("$.profileImageUrl").doesNotExist())
            .andExpect(jsonPath("$.gender").doesNotExist())
            .andExpect(jsonPath("$.birthDate").doesNotExist())
            .andExpect(jsonPath("$.location").doesNotExist())
            .andExpect(jsonPath("$.temperatureSensitivity").doesNotExist());

        then(userService).should().getUserProfile(userId);
    }

    @Test
    @WithMockUser
    void 존재하지_않는_사용자_프로필_조회시_404_에러_발생() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND);
        given(userService.getUserProfile(userId)).willThrow(exception);

        // when
        ResultActions result = performGetUserProfileRequest(userId);

        // then
        result.andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));

        then(userService).should().getUserProfile(userId);
    }

    @Test
    @WithMockUser
    void 계정_목록_조회_성공_createdAt_DESC() throws Exception {
        // given
        List<UserDto> data = List.of(
            createUserAccountDto("test@test1.com","testUser1",Role.USER,false),
            createUserAccountDto("test@test2.com","testUser2",Role.USER,false),
            createUserAccountDto("test@test3.com","testUser3",Role.USER,false)
        );
        CursorPageResponse<UserDto> response = new CursorPageResponse<>(
            data,
            "CUR_NEXT",
            UUID.randomUUID().toString(),
            true,
            123L,
            "createdAt",
            "DESCENDING"
        );

        given(userService.listUsers(any(UserListQueryParams.class))).willReturn(response);

        // when
        ResultActions result = mockMvc.perform(get("/api/users")
            .param("limit","3")
            .param("sortBy","createdAt")
            .param("sortDirection","DESCENDING")
            .accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.totalCount").value(123))
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));

        ArgumentCaptor<UserListQueryParams> captor = ArgumentCaptor.forClass(UserListQueryParams.class);
        then(userService).should().listUsers(captor.capture());

        UserListQueryParams captured = captor.getValue();
        assertThat(captured.cursor()).isNull();
        assertThat(captured.limit()).isEqualTo(3);
        assertThat(captured.sortBy()).isEqualTo("createdAt");
        assertThat(captured.sortDirection()).isEqualTo("DESCENDING");
        assertThat(captured.emailLike()).isNull();
        assertThat(captured.roleEqual()).isNull();
        assertThat(captured.locked()).isNull();
    }

    @Test
    @WithMockUser
    void 계정_목록_조회_성공_email_ASC_필터_전달() throws Exception {
        // given
        List<UserDto> data = List.of(
            createUserAccountDto("test@test1.com","testUser1",Role.USER,false),
            createUserAccountDto("test@test2.com","testUser2",Role.USER,false)
        );
        CursorPageResponse<UserDto> response = new CursorPageResponse<>(
            data,
            "CUR2",
            UUID.randomUUID().toString(),
            false,
            2L,
            "email",
            "ASCENDING"
        );

        given(userService.listUsers(any(UserListQueryParams.class))).willReturn(response);

        // when
        ResultActions result = mockMvc.perform(get("/api/users")
            .param("cursor","CUR1")
            .param("limit","2")
            .param("sortBy","email")
            .param("sortDirection","ASCENDING")
            .param("emailLike","test")
            .param("roleEqual","USER")
            .param("locked","false")
        );

        // then

        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalCount").value(2))
            .andExpect(jsonPath("$.sortBy").value("email"))
            .andExpect(jsonPath("$.sortDirection").value("ASCENDING"));

        ArgumentCaptor<UserListQueryParams> captor = ArgumentCaptor.forClass(UserListQueryParams.class);
        then(userService).should().listUsers(captor.capture());

        UserListQueryParams captured = captor.getValue();
        assertThat(captured.cursor()).isEqualTo("CUR1");
        assertThat(captured.limit()).isEqualTo(2);
        assertThat(captured.sortBy()).isEqualTo("email");
        assertThat(captured.sortDirection()).isEqualTo("ASCENDING");
        assertThat(captured.emailLike()).isEqualTo("test");
        assertThat(captured.roleEqual()).isEqualTo("USER");
        assertThat(captured.locked()).isFalse();
    }

    @Test
    @WithMockUser
    void limit_누락시_기본_값으로_계정_목록_조회_성공() throws Exception {
        // given
        CursorPageResponse<UserDto> response = new CursorPageResponse<>(
            List.of(),
            null,
            null,
            false,
            0L,
            "createdAt",
            "DESCENDING"
        );
        given(userService.listUsers(any(UserListQueryParams.class))).willReturn(response);

        // when
        ResultActions result = mockMvc.perform(get("/api/users"));

        result.andExpect(status().isOk());
        ArgumentCaptor<UserListQueryParams> captor = ArgumentCaptor.forClass(UserListQueryParams.class);
        then(userService).should().listUsers(captor.capture());

        UserListQueryParams captured = captor.getValue();
        assertThat(captured.cursor()).isNull();
        assertThat(captured.limit()).isEqualTo(20);
        assertThat(captured.sortBy()).isEqualTo("createdAt");
        assertThat(captured.sortDirection()).isEqualTo("DESCENDING");
        assertThat(captured.emailLike()).isNull();
        assertThat(captured.roleEqual()).isNull();
        assertThat(captured.locked()).isNull();
    }

    @Test
    @WithMockUser
    void 정렬값이_유효하지_않아_계정_목록_조회_실패() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(
            get("/api/users")
                .param("limit","10")
                .param("sortBy","invalid")
                .param("sortDirection","DOWN")
        );

        // then
        result.andExpect(status().isBadRequest())
            .andExpect(res -> assertThat(res.getResolvedException())
                .isInstanceOf(MethodArgumentNotValidException.class));
        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser
    void 프로필_업데이트_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        ProfileDto profileDto = createCompleteProfileDto(userId);
        given(userService.updateUserProfile(any(UUID.class), any(ProfileUpdateRequest.class),any()))
            .willReturn(profileDto);

        MockMultipartFile requestPart = createProfileUpdatePart();
        MockMultipartFile imagePart = new MockMultipartFile(
            "image", "profile.png", "image/png", "fake".getBytes()
        );

        // when
        ResultActions result = mockMvc.perform(
            multipart("/api/users/{userId}/profiles",userId)
                .file(requestPart)
                .file(imagePart)
                .with(request -> { request.setMethod("PATCH"); return request; })
                .with(csrf())
                .with(authenticatedUser(userId, Role.USER))
        );

        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(profileDto.userId().toString()))
            .andExpect(jsonPath("$.name").value(profileDto.name()))
            .andExpect(jsonPath("$.temperatureSensitivity").value(profileDto.temperatureSensitivity()));
        then(userService).should().updateUserProfile(any(UUID.class), any(ProfileUpdateRequest.class), any());
    }

    @Test
    @WithMockUser
    void 프로필_업데이트_실패_서비스_예외_전파() throws Exception {
       // given
       UUID userId = UUID.randomUUID();
       CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND);
       given(userService.updateUserProfile(any(UUID.class), any(ProfileUpdateRequest.class), any()))
           .willThrow(exception);

       MockMultipartFile requestPart = createProfileUpdatePart();

       // when
        ResultActions result = mockMvc.perform(
            multipart("/api/users/{userId}/profiles",userId)
                .file(requestPart)
                .with(request -> { request.setMethod("PATCH"); return request; })
                .with(csrf())
                .with(authenticatedUser(userId, Role.USER))
        );

        // then
        result.andExpect(status().isNotFound());
        then(userService).should().updateUserProfile(any(UUID.class), any(ProfileUpdateRequest.class), any());
    }
}