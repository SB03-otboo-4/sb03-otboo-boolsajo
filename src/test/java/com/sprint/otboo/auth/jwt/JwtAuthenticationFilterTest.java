package com.sprint.otboo.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.auth.InvalidTokenException;
import com.sprint.otboo.common.exception.auth.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenProvider tokenProvider;
    @Mock
    private JwtRegistry jwtRegistry;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_토큰이_제공되면_SecurityContext에_인증_정보가_저장된다()
        throws ServletException, IOException, ParseException {
        // given
        String validToken = "valid-access-token";
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtRegistry.isAccessTokenValid(validToken)).thenReturn(true);
        when(tokenProvider.getAuthentication(validToken)).thenReturn(authentication);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 토큰이_없으면_SecurityContext는_비어있다() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 만료된_토큰이_제공되면_EXPIRED_TOKEN_에러_코드가_request에_저장된다() throws Exception {
        // given
        String expiredToken = "expired-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
        when(jwtRegistry.isAccessTokenValid(expiredToken)).thenReturn(true);
        doThrow(new TokenExpiredException()).when(tokenProvider).validateAccessToken(expiredToken);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(request).setAttribute("exception", ErrorCode.EXPIRED_TOKEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 유효하지_않은_토큰이_제공되면_INVALID_TOKEN_에러_코드가_request에_저장된다() throws Exception {
        // given
        String invalidToken = "invalid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtRegistry.isAccessTokenValid(invalidToken)).thenReturn(true);
        doThrow(new InvalidTokenException()).when(tokenProvider).validateAccessToken(invalidToken);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(request).setAttribute("exception", ErrorCode.INVALID_TOKEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}