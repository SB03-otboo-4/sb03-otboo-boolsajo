package com.sprint.otboo.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
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
}