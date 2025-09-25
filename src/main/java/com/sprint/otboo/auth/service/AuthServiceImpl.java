package com.sprint.otboo.auth.service;

import com.nimbusds.jose.JOSEException;
import com.sprint.otboo.auth.dto.AuthResultDto;
import com.sprint.otboo.auth.dto.JwtInformation;
import com.sprint.otboo.auth.dto.SignInRequest;
import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.auth.util.MailService;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.auth.AccountLockedException;
import com.sprint.otboo.common.exception.auth.InvalidCredentialsException;
import com.sprint.otboo.common.exception.auth.InvalidTokenException;
import com.sprint.otboo.common.exception.auth.TokenCreationException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final JwtRegistry jwtRegistry;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;

    @Override
    public AuthResultDto signIn(SignInRequest request) {
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

        if (passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            // 3. 로그인 성공: 토큰 발급
            CustomUserDetails customDetails = (CustomUserDetails) userDetails;
            UserDto userDto = customDetails.getUserDto();
            try {
                String accessToken = tokenProvider.createAccessToken(userDto);
                String refreshToken = tokenProvider.createRefreshToken(userDto);

                jwtRegistry.register(new JwtInformation(userDto, accessToken, refreshToken));
                return new AuthResultDto(userDto, accessToken, refreshToken);
            } catch (JOSEException e) {
                throw new TokenCreationException(ErrorCode.TOKEN_CREATION_FAILED);
            }
        }

        String redisKey = "temp_pw:" + request.username();
        String tempPasswordHash = redisTemplate.opsForValue().get(redisKey);

        if (tempPasswordHash != null && passwordEncoder.matches(request.password(), tempPasswordHash)) {
            // 4. 임시 로그인 성공: 토큰 발급
            CustomUserDetails customDetails = (CustomUserDetails) userDetails;
            UserDto userDto = customDetails.getUserDto();
            try {
                String accessToken = tokenProvider.createAccessToken(userDto);
                String refreshToken = tokenProvider.createRefreshToken(userDto);

                jwtRegistry.register(new JwtInformation(userDto, accessToken, refreshToken));

                // 임시 비밀번호 즉시 삭제
                redisTemplate.delete(redisKey);

                return new AuthResultDto(userDto, accessToken, refreshToken);
            } catch (JOSEException e) {
                throw new TokenCreationException(ErrorCode.TOKEN_CREATION_FAILED);
            }
        }

        // 기존, 임시 비밀번호 모두 일치하지 않음
        throw new InvalidCredentialsException();
    }

    @Override
    public AuthResultDto reissueToken(String refreshToken) throws ParseException {
        if (!jwtRegistry.isRefreshTokenValid(refreshToken)) {
            throw new InvalidTokenException();
        }

        // 1. Refresh Token 검증
        tokenProvider.validateRefreshToken(refreshToken);

        // 2. 토큰에서 사용자 정보 추출
        String email = tokenProvider.getEmailFromRefreshToken(refreshToken);

        // 3. DB에서 사용자 정보 조회 및 검증
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!userDetails.isAccountNonLocked()) {
            CustomUserDetails customDetails = (CustomUserDetails) userDetails;
            throw AccountLockedException.withId(customDetails.getUserId());
        }

        // 4. 새로운 토큰들 생성
        CustomUserDetails customDetails = (CustomUserDetails) userDetails;
        UserDto userDto = customDetails.getUserDto();
        try {
            String newAccessToken = tokenProvider.createAccessToken(userDto);
            String newRefreshToken = tokenProvider.createRefreshToken(userDto);

            jwtRegistry.invalidate(refreshToken);
            jwtRegistry.register(new JwtInformation(userDto, newAccessToken, newRefreshToken));
            return new AuthResultDto(userDto, newAccessToken, newRefreshToken);
        } catch (JOSEException e) {
            throw new TokenCreationException(ErrorCode.TOKEN_CREATION_FAILED);
        }
    }

    @Override
    public void signOut(String refreshToken) throws ParseException {
        tokenProvider.validateRefreshToken(refreshToken);
        jwtRegistry.invalidate(refreshToken);
    }


    @Override
    @Transactional
    public void sendTemporaryPassword(String email) {
        // 1. 유저 확인
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> UserNotFoundException.withEmail(email));

        // 2. 임시 비밀번호 생성
        String tempPassword = generateRandomPassword(10);

        // 3. Redis에 해싱하여 저장
        String hashedPassword = passwordEncoder.encode(tempPassword);
        redisTemplate.opsForValue().set("temp_pw:" + email, hashedPassword, Duration.ofMinutes(3));

        // 4. MailService를 통해 메일 발송 위임
        mailService.sendTemporaryPasswordEmail(email, tempPassword);
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}