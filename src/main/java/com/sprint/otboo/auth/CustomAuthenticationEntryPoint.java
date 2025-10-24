package com.sprint.otboo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.common.dto.ErrorResponse;
import com.sprint.otboo.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Spring Security에서 인증되지 않은 사용자의 요청 처리 시,
 * 커스텀 에러 응답을 생성한다
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * 인증 실패 시 호출되어, ErrorCode에 맞는 JSON 에러 응답을 생성한다
     *
     * @param request       요청 객체
     * @param response      응답 객체
     * @param authException 인증 과정에서 발생한 예외
     * @throws IOException 응답 작성 중 I/O 오류 발생 시
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException authException) throws IOException, ServletException {

        ErrorCode errorCode = (ErrorCode) request.getAttribute("exception");
        if (errorCode == null) {
            errorCode = ErrorCode.AUTHENTICATION_FAILED;
        }

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(errorCode);

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
    }
}
