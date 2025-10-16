package com.sprint.otboo.auth.jwt;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.auth.InvalidTokenException;
import com.sprint.otboo.common.exception.auth.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 매 요청마다 HTTP 헤더에서 JWT(JSON Web Token)를 확인하여,
 * 유효한 경우 SecurityContext에 인증 정보를 설정한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final JwtRegistry jwtRegistry;

    /**
     * 요청 헤더에서 Access Token을 추출하고 유효성을 검증한 뒤, SecurityContext에 인증 정보를 설정한다.
     * 토큰 검증 실패 시 발생하는 예외는 request attribute에 저장하여
     * AuthenticationEntryPoint에서 사용하도록 전달한다.
     *
     * @param request     요청 객체
     * @param response    응답 객체
     * @param filterChain 필터 체인
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token) && jwtRegistry.isAccessTokenValid(token)) {
                tokenProvider.validateAccessToken(token);
                Authentication authentication = tokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (TokenExpiredException e) {
            request.setAttribute("exception", ErrorCode.EXPIRED_TOKEN);
        } catch (InvalidTokenException | ParseException e) {
            request.setAttribute("exception", ErrorCode.INVALID_TOKEN);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
