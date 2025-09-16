package com.sprint.otboo.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.ChangePasswordRequest;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
import com.sprint.otboo.user.dto.request.UserLockUpdateRequest;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.service.UserService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(UserController.class)
@DisplayName("UserController 테스트")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
}