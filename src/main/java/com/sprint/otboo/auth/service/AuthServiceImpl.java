package com.sprint.otboo.auth.service;

import com.nimbusds.jose.JOSEException;
import com.sprint.otboo.auth.dto.JwtDto;
import com.sprint.otboo.auth.dto.SignInRequest;
import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.auth.AccountLockedException;
import com.sprint.otboo.common.exception.auth.InvalidCredentialsException;
import com.sprint.otboo.common.exception.auth.TokenCreationException;
import com.sprint.otboo.user.dto.data.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    public JwtDto signIn(SignInRequest request) {
        // 1. 사용자 조회 및 기본 검증
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(request.username());
        } catch (UsernameNotFoundException e) {
            throw new InvalidCredentialsException();
        }

        // 2. 계정 상태 검증
        if (!userDetails.isAccountNonLocked()) {
            CustomUserDetails customDetails = (CustomUserDetails) userDetails;
            throw AccountLockedException.withId(customDetails.getUserId());
        }

        // 3. 비밀번호 일치 비교
        if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // 4. AccessToken 발급
        CustomUserDetails customDetails = (CustomUserDetails) userDetails;
        UserDto userDto = customDetails.getUserDto();
        String accessToken;
        try {
            accessToken = tokenProvider.createAccessToken(userDto);
        } catch (JOSEException e) {
            throw new TokenCreationException(ErrorCode.TOKEN_CREATION_FAILED);
        }

        return new JwtDto(userDto, accessToken);
    }
}
