package com.sprint.otboo.auth.service;

import com.sprint.otboo.auth.dto.JwtDto;
import com.sprint.otboo.auth.dto.SignInRequest;

public interface AuthService {
    JwtDto signIn(SignInRequest request);
    JwtDto reissueToken(String accessToken);
}
