package com.sprint.otboo.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("csrf-token")
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        csrfToken.getToken();

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
