package com.sprint.otboo.auth.controller;

import com.sprint.otboo.auth.dto.AuthResultDto;
import com.sprint.otboo.auth.dto.JwtDto;
import com.sprint.otboo.auth.dto.SignInRequest;
import com.sprint.otboo.auth.jwt.RefreshTokenCookieUtil;
import com.sprint.otboo.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.text.ParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieUtil cookieUtil;

    @GetMapping("/csrf-token")
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        csrfToken.getToken();

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }

    @PostMapping(
        value = "/sign-in",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JwtDto> signIn(@Valid @ModelAttribute SignInRequest request, HttpServletResponse response) {
        AuthResultDto authResult = authService.signIn(request);

        response.addCookie(cookieUtil.createRefreshTokenCookie(authResult.refreshToken()));
        JwtDto dto = new JwtDto(authResult.userDto(), authResult.accessToken());

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(dto);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refreshToken(@CookieValue("REFRESH_TOKEN")String refreshToken, HttpServletResponse response)
        throws ParseException {

        AuthResultDto authResult = authService.reissueToken(refreshToken);

        response.addCookie(cookieUtil.createRefreshTokenCookie(authResult.refreshToken()));
        JwtDto dto = new JwtDto(authResult.userDto(), authResult.accessToken());

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(dto);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> SignOut(@CookieValue("REFRESH_TOKEN") String refreshToken, HttpServletResponse response)
        throws ParseException {
        authService.signOut(refreshToken);

        response.addCookie(cookieUtil.createExpiredCookie());

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }
}
