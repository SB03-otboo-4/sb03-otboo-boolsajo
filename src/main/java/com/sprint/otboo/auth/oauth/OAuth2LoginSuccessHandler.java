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

    /**
     * OAuth2 인증 성공 시 호출된다.
     * Access Token과 Refresh Token을 생성하여 세션을 등록하고,
     * Refresh Token은 쿠키에 담아 응답하며, 사용자를 홈("/")으로 리디렉션한다.
     *
     * @param request        요청 객체
     * @param response       응답 객체
     * @param authentication Spring Security가 생성한 인증 객체
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        UserDto userDto = customOAuth2User.getUserDto();
        log.info("OAuth2 로그인 성공 사용자: {}", userDto.name());

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
