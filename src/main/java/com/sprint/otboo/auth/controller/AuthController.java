package com.sprint.otboo.auth.controller;

import com.sprint.otboo.auth.dto.AuthResultDto;
import com.sprint.otboo.auth.dto.JwtDto;
import com.sprint.otboo.auth.dto.ResetPasswordRequest;
import com.sprint.otboo.auth.dto.SignInRequest;
import com.sprint.otboo.auth.jwt.RefreshTokenCookieUtil;
import com.sprint.otboo.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.text.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController implements AuthApi{

    private final AuthService authService;
    private final RefreshTokenCookieUtil cookieUtil;

    /**
     * CSRF 토큰 발급
     * @param csrfToken Spring Security가 주입하는 CSRF 토큰 객체
     * @return 204 No Content
     */
    @Override
    @GetMapping("/csrf-token")
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        csrfToken.getToken();

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }

    /**
     * 사용자 로그인을 처리하고 JWT 발급
     * @param request  로그인 정보 (username, password)
     * @param response Refresh Token을 쿠키에 담기 위한 응답 객체
     * @return 사용자 정보와 Access Token
     */
    @Override
    @PostMapping(
        value = "/sign-in",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JwtDto> signIn(@Valid @ModelAttribute SignInRequest request, HttpServletResponse response) {
        log.info("로그인 요청 사용자명: {}", request.username());
        AuthResultDto authResult = authService.signIn(request);

        response.addCookie(cookieUtil.createRefreshTokenCookie(authResult.refreshToken()));
        JwtDto dto = new JwtDto(authResult.userDto(), authResult.accessToken());

        log.debug("로그인 성공 responseDto={}", dto);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(dto);
    }

    /**
     * Access Token 재발급
     * @param refreshToken 쿠키에 담긴 Refresh Token
     * @param response     새로운 Refresh Token을 쿠키에 담기 위한 응답 객체
     * @return 새로운 사용자 정보와 Access Token
     * @throws ParseException Refresh Token 파싱 실패 시
     */
    @Override
    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refreshToken(@CookieValue("REFRESH_TOKEN")String refreshToken, HttpServletResponse response)
        throws ParseException {
        log.info("토큰 재발급 요청");
        AuthResultDto authResult = authService.reissueToken(refreshToken);

        response.addCookie(cookieUtil.createRefreshTokenCookie(authResult.refreshToken()));
        JwtDto dto = new JwtDto(authResult.userDto(), authResult.accessToken());

        log.info("토큰 재발급 성공. username={}", authResult.userDto().name());
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(dto);
    }

    /**
     * 사용자 로그아웃 처리
     * @param refreshToken 쿠키에 담긴 Refresh Token
     * @param response     만료된 쿠키를 전달하기 위한 응답 객체
     * @return 204 No Content
     * @throws ParseException Refresh Token 파싱 실패 시
     */
    @Override
    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(@CookieValue("REFRESH_TOKEN") String refreshToken, HttpServletResponse response)
        throws ParseException {
        log.info("로그아웃 요청");
        authService.signOut(refreshToken);

        response.addCookie(cookieUtil.createExpiredCookie());

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }

    /**
     * 임시 비밀번호를 발급하여 이메일로 전송
     * @param requestDto 비밀번호를 초기화할 사용자의 이메일
     * @return 200 OK
     */
    @Override
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest requestDto) {
        log.info("비밀번호 초기화 요청: email={}", requestDto.email());
        authService.sendTemporaryPassword(requestDto.email());

        return ResponseEntity
            .status(HttpStatus.OK)
            .build();
    }
}
