package com.sprint.otboo.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.CustomAuthenticationEntryPoint;
import com.sprint.otboo.auth.dto.AuthResultDto;
import com.sprint.otboo.auth.dto.SignInRequest;
import com.sprint.otboo.auth.jwt.JwtAuthenticationFilter;
import com.sprint.otboo.auth.jwt.RefreshTokenCookieUtil;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.auth.service.AuthService;
import com.sprint.otboo.common.config.SecurityConfig;
import com.sprint.otboo.common.exception.GlobalExceptionHandler;
import com.sprint.otboo.common.exception.auth.InvalidCredentialsException;
import com.sprint.otboo.common.exception.auth.InvalidTokenException;
import com.sprint.otboo.common.exception.auth.TokenExpiredException;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, RefreshTokenCookieUtil.class})
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
        ResultActions resultActions = mockMvc.perform(get("/api/auth/csrf-token"));

        // then
        MvcResult mvcResult = resultActions.andExpect(status().isNoContent()).andReturn();

        var setCookies = mvcResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        String xsrf = setCookies.stream()
            .filter(v -> v.startsWith("XSRF-TOKEN="))
            .findFirst().orElse(null);
        assertThat(xsrf).as("XSRF-TOKEN Set-Cookie").isNotNull();
        assertThat(xsrf).doesNotContain("HttpOnly");
    }

    @Test
    void 로그인_성공_CSRF_보호_미적용() throws Exception {
        //given
        UserDto userDto = new UserDto(UUID.randomUUID(), Instant.now(), "test@abc.com", "t1", Role.USER, LoginType.GENERAL, false);
        AuthResultDto authResult = new AuthResultDto(userDto, "access.jwt.token", "refresh.jwt.token");
        given(authService.signIn(any(SignInRequest.class))).willReturn(authResult);

        // when
        ResultActions resultActions = mockMvc.perform(multipart("/api/auth/sign-in")
            .param("username", "test@abc.com")
            .param("password", "1234")
            .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isOk());
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

        AuthResultDto authResult = new AuthResultDto(userDto, "access.jwt.token", "refresh.jwt.token");

        given(authService.signIn(any(SignInRequest.class))).willReturn(authResult);

        // when
        ResultActions resultActions = mockMvc.perform(multipart("/api/auth/sign-in")
            .with(csrf())
            .param("username", "test@abc.com")
            .param("password", "1234")
            .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
            .andExpect(jsonPath("$.userDto.id").value(userId.toString()))
            .andExpect(jsonPath("$.userDto.email").value("test@abc.com"))
            .andExpect(jsonPath("$.userDto.name").value("t1"))
            .andExpect(jsonPath("$.userDto.role").value("USER"));

        resultActions.andExpect(cookie().exists("REFRESH_TOKEN"))
            .andExpect(cookie().httpOnly("REFRESH_TOKEN", true))
            .andExpect(cookie().value("REFRESH_TOKEN", "refresh.jwt.token"));
    }

    @Test
    void 로그인_실패시_401_반환() throws Exception {
        //given
        given(authService.signIn(any(SignInRequest.class)))
            .willThrow(new InvalidCredentialsException());

        // when
        ResultActions resultActions = mockMvc.perform(multipart("/api/auth/sign-in")
            .with(csrf())
            .param("username", "test@abc.com")
            .param("password", "wrong")
            .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isUnauthorized());
    }

    @Test
    void 로그인_username_누락시_400_반환() throws Exception {
        // when
        ResultActions resultActions = mockMvc.perform(multipart("/api/auth/sign-in")
            .with(csrf())
            .param("password", "1234")
            .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    void 로그인_JSON으로_보내면_415_반환() throws Exception {
        //given
        String json =
          """
          {"username":"test@abc.com","password":"1234"}
          """;

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/sign-in")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
            .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void 토큰재발급_성공() throws Exception {
        // given
        String refreshToken = "valid.refresh.jwt.token";
        UUID userId = UUID.randomUUID();
        UserDto newUserDto = new UserDto(userId, Instant.now(), "test@abc.com", "t1", Role.USER, LoginType.GENERAL, false);
        AuthResultDto authResult = new AuthResultDto(newUserDto, "new.access.jwt.token", "new.refresh.token");

        given(authService.reissueToken(refreshToken)).willReturn(authResult);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/refresh")
            .with(csrf())
            .cookie(new Cookie("REFRESH_TOKEN", refreshToken))
            .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("new.access.jwt.token"))
            .andExpect(jsonPath("$.userDto.id").value(userId.toString()));

        resultActions.andExpect(cookie().exists("REFRESH_TOKEN"))
            .andExpect(cookie().httpOnly("REFRESH_TOKEN", true))
            .andExpect(cookie().path("REFRESH_TOKEN", "/api/auth/refresh"));
    }

    @Test
    void 토큰재발급_실패_유효하지않은_토큰() throws Exception {
        // given
        String invalidRefreshToken = "invalid.refresh.jwt.token";

        given(authService.reissueToken(invalidRefreshToken)).willThrow(new InvalidTokenException());

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/refresh")
            .with(csrf())
            .cookie(new Cookie("REFRESH_TOKEN", invalidRefreshToken))
            .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isUnauthorized());
    }

    @Test
    void 토큰재발급_실패_쿠키가_없는_경우() throws Exception {
        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/refresh")
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    void 토큰재발급_실패_만료된_토큰() throws Exception {
        // given
        String expiredRefreshToken = "expired.refresh.jwt.token";

        given(authService.reissueToken(expiredRefreshToken))
            .willThrow(new TokenExpiredException());

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/refresh")
            .with(csrf())
            .cookie(new Cookie("REFRESH_TOKEN", expiredRefreshToken))
            .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isUnauthorized());
    }

}
