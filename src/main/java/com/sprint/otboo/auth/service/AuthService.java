package com.sprint.otboo.auth.service;

import com.sprint.otboo.auth.dto.AuthResultDto;
import com.sprint.otboo.auth.dto.SignInRequest;
import java.text.ParseException;

public interface AuthService {
    AuthResultDto signIn(SignInRequest request);
    AuthResultDto reissueToken(String accessToken) throws ParseException;
}
