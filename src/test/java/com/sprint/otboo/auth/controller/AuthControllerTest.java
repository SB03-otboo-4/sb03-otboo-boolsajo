package com.sprint.otboo.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.CustomAuthenticationEntryPoint;
import com.sprint.otboo.auth.dto.JwtDto;
import com.sprint.otboo.auth.dto.SignInRequest;
import com.sprint.otboo.auth.jwt.JwtAuthenticationFilter;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.auth.service.AuthService;
import com.sprint.otboo.common.config.SecurityConfig;
import com.sprint.otboo.common.exception.GlobalExceptionHandler;
import com.sprint.otboo.common.exception.auth.InvalidCredentialsException;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithAnonymousUser
@AutoConfigureMockMvc(addFilters = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    public void csrf토큰_조회_성공() throws Exception {
        // when
        var result = mockMvc.perform(get("/api/auth/csrf-token"))
            .andExpect(status().isNoContent())
            .andReturn();

        // then
        var setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        String xsrf = setCookies.stream()
            .filter(v -> v.startsWith("XSRF-TOKEN="))
            .findFirst().orElse(null);
        assertThat(xsrf).as("XSRF-TOKEN Set-Cookie").isNotNull();
        assertThat(xsrf).doesNotContain("HttpOnly");
    }

    @Test
    void 로그인_성공_CSRF_보호_미적용() throws Exception {
        // when & then
        mockMvc.perform(multipart("/api/auth/sign-in")
                .param("username", "test@abc.com")
                .param("password", "1234"))
            .andExpect(status().isOk());
    }

    @Test
    void 로그인_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        UserDto userDto = new UserDto(
            userId,
            Instant.now(),
            "test@abc.com",
            "t1",
            Role.USER,
            LoginType.GENERAL,
            false
        );

        JwtDto jwt = new JwtDto(userDto, "access.jwt.token");

        given(authService.signIn(any(SignInRequest.class))).willReturn(jwt);

         //when & then
        mockMvc.perform(multipart("/api/auth/sign-in")
                .with(csrf())
                .param("username", "test@abc.com")
                .param("password", "1234")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
            .andExpect(jsonPath("$.userDto.id").value(userId.toString()))
            .andExpect(jsonPath("$.userDto.email").value("test@abc.com"))
            .andExpect(jsonPath("$.userDto.name").value("t1"))
            .andExpect(jsonPath("$.userDto.role").value("USER"))
            .andExpect(jsonPath("$.userDto.linkedOAuthProviders").value("GENERAL"))
            .andExpect(jsonPath("$.userDto.locked").value(false));
    }

    @Test
    void 로그인_실패시_401_반환() throws Exception {
        given(authService.signIn(any(SignInRequest.class)))
            .willThrow(new InvalidCredentialsException());

        mockMvc.perform(multipart("/api/auth/sign-in")
                .with(csrf())
                .param("username", "test@abc.com")
                .param("password", "wrong")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 로그인_username_누락시_400_반환() throws Exception {
        mockMvc.perform(multipart("/api/auth/sign-in")
                .with(csrf())
                .param("password", "1234")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void 로그인_JSON으로_보내면_415_반환() throws Exception {
        String json =
          """
          {"username":"test@abc.com","password":"1234"}
          """;
        mockMvc.perform(post("/api/auth/sign-in")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnsupportedMediaType());
    }

}
