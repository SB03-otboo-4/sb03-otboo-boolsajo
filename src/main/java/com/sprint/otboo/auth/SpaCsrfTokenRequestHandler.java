package com.sprint.otboo.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Single Page Application 환경에서 CSRF 토큰을 처리한다.
 */
public class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    /**
     * CSRF 토큰을 생성하고 응답에 포함시킨다.
     * 내부적으로 XorCsrfTokenRequestAttributeHandler를 위임하여 토큰을 쿠키에 저장한다.
     *
     * @param request   요청 객체
     * @param response  응답 객체
     * @param csrfToken CSRF 토큰 공급자
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       Supplier<CsrfToken> csrfToken) {
        this.xor.handle(request, response, csrfToken);

        csrfToken.get();
    }

    /**
     * 요청에서 CSRF 토큰 값을 해석한다.
     * HTTP 헤더에 토큰이 있으면 일반 핸들러로, 없으면 XOR 핸들러로 처리한다.
     *
     * @param request   요청 객체
     * @param csrfToken 서버에 저장된 CSRF 토큰
     * @return 요청에서 추출한 CSRF 토큰 값
     */
    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());

        return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request,
                csrfToken);
    }
}
