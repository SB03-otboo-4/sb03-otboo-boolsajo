package com.sprint.otboo.auth.oauth;

import com.nimbusds.jose.JOSEException;
import com.sprint.otboo.auth.dto.JwtInformation;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.RefreshTokenCookieUtil;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.user.dto.data.UserDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenProvider jwtTokenProvider;
    private final RefreshTokenCookieUtil refreshTokenCookieUtil;
    private final JwtRegistry jwtRegistry;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        UserDto userDto = customOAuth2User.getUserDto();

        String accessToken;
        String refreshToken;
        try {
            accessToken = jwtTokenProvider.createAccessToken(userDto);
            refreshToken = jwtTokenProvider.createRefreshToken(userDto);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        jwtRegistry.register(new JwtInformation(userDto, accessToken, refreshToken));

        Cookie refreshTokenCookie = refreshTokenCookieUtil.createRefreshTokenCookie(refreshToken);
        response.addCookie(refreshTokenCookie);

        response.sendRedirect("/");
    }
}
