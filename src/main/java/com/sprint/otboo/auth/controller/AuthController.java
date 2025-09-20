package com.sprint.otboo.auth.controller;

import com.sprint.otboo.auth.dto.JwtDto;
import com.sprint.otboo.auth.dto.SignInRequest;
import com.sprint.otboo.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
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
    public ResponseEntity<JwtDto> signIn(@Valid @ModelAttribute SignInRequest request) {
        JwtDto dto = authService.signIn(request);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(dto);
    }
}
