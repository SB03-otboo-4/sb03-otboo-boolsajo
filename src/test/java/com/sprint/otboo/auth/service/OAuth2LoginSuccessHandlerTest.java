package com.sprint.otboo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JOSEException;
import com.sprint.otboo.auth.dto.JwtInformation;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.RefreshTokenCookieUtil;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.auth.oauth.CustomOAuth2User;
import com.sprint.otboo.auth.oauth.OAuth2LoginSuccessHandler;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private TokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenCookieUtil cookieUtil;
    @Mock
    private JwtRegistry jwtRegistry;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private OAuth2LoginSuccessHandler successHandler;

    private void setupMockAuthentication() {
        UserDto testUserDto = new UserDto(UUID.randomUUID(), null, "testuser", "test@test.com", Role.USER, null, false);
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(testUserDto, Map.of(), "sub");
        when(authentication.getPrincipal()).thenReturn(customOAuth2User);
    }

    @Test
    void 소셜로그인_인증성공_토큰_쿠키_리다이렉트_성공() throws Exception {
        // given
        setupMockAuthentication();

        String expectedAccessToken = "fake-access-token-123";
        String expectedRefreshToken = "fake-refresh-token-456";
        Cookie mockRefreshTokenCookie = new Cookie("REFRESH_TOKEN", expectedRefreshToken);

        when(jwtTokenProvider.createAccessToken(any(UserDto.class))).thenReturn(expectedAccessToken);
        when(jwtTokenProvider.createRefreshToken(any(UserDto.class))).thenReturn(expectedRefreshToken);
        when(cookieUtil.createRefreshTokenCookie(expectedRefreshToken)).thenReturn(mockRefreshTokenCookie);
        doNothing().when(jwtRegistry).register(any(JwtInformation.class));

        // when
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        ArgumentCaptor<JwtInformation> jwtInfoCaptor = ArgumentCaptor.forClass(JwtInformation.class);
        verify(jwtRegistry).register(jwtInfoCaptor.capture());
        assertThat(jwtInfoCaptor.getValue().accessToken()).isEqualTo(expectedAccessToken);

        verify(response, times(1)).addCookie(any(Cookie.class));
        verify(response).sendRedirect("/");
    }

    @Test
    void 소셜로그인_인증성공_토큰_생성_실패() throws Exception {
        // given
        setupMockAuthentication();
        when(jwtTokenProvider.createAccessToken(any(UserDto.class)))
            .thenThrow(new JOSEException("Token generation error"));

        // when
        Throwable thrown = catchThrowable(() -> {
            successHandler.onAuthenticationSuccess(request, response, authentication);
        });

        // then
        assertThat(thrown)
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(JOSEException.class);

        verify(jwtRegistry, never()).register(any());
        verify(response, never()).addCookie(any());
        verify(response, never()).sendRedirect(anyString());
    }
}