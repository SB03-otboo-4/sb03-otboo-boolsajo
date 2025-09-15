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

    @Test
    @WithMockUser
    void 사용자_등록에_성공() throws Exception {
        // given
        UserCreateRequest request = new UserCreateRequest(
            "testUser",
            "test@test.com",
            "test1234"
        );

        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        UserDto responseDto = new UserDto(
            userId,
            now,
            request.email(),
            request.name(),
            Role.USER,
            LoginType.GENERAL,
            false
        );

        given(userService.createUser(any(UserCreateRequest.class))).willReturn(responseDto);

        // when
        ResultActions result = mockMvc.perform(post("/api/users")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(userId.toString()))
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
    void 유효하지_않은_이메일로_사용자_등록_시_400_에러_발생() throws Exception {
        // given
        UserCreateRequest request = new UserCreateRequest(
            "testUser",
            "test-testcom",
            "test1234"
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/users")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());

        then(userService).should(never()).createUser(any(UserCreateRequest.class));
    }


    @Test
    @WithMockUser
    void 빈_이름으로_사용자_등록_시_400_에러_발생() throws Exception {
        // given
        UserCreateRequest request = new UserCreateRequest(
            "",
            "test@test.com",
            "test1234"
        );

        // When
        ResultActions result = mockMvc.perform(post("/api/users")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isBadRequest());

        then(userService).should(never()).createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser
    void 유효하지_않은_비밀번호_길이로_인한_400_에러_발생() throws Exception {
        // given
        UserCreateRequest request = new UserCreateRequest(
            "testUser",
            "test@test.com",
            "test123"
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/users")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isBadRequest());

        then(userService).should(never()).createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser
    void 중복된_이메일로_사용자_등록_시_400_에러_발생() throws Exception {
        // given
        UserCreateRequest request = new UserCreateRequest(
            "testUser",
            "test@test.com",
            "test1234"
        );

        given(userService.createUser(any(UserCreateRequest.class)))
            .willThrow(new IllegalArgumentException("이미 사용 중인 이메일입니다 : test@test.com"));

        // when
        ResultActions result = mockMvc.perform(post("/api/users")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다 : test@test.com"));

        then(userService).should().createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser
    void 잘못된_JSON_형식으로_요청_시_400_에러_발생() throws Exception {
        // given
        String invalidJson = "{invalid json}";

        // when
        ResultActions result = mockMvc.perform(post("/api/users")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson));

        // then
        result.andExpect(status().isBadRequest());

        then(userService).should(never()).createUser(any(UserCreateRequest.class));
    }
}
