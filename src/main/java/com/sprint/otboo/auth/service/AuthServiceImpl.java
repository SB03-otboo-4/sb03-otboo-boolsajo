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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService{

    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final JwtRegistry jwtRegistry;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;

    /**
     * 사용자 로그인을 처리하고 인증 토큰 발급
     * 일반 비밀번호 또는 Redis에 저장된 임시 비밀번호로 인증 시도
     * @param request 로그인 요청 정보 (username, password)
     * @return 인증 결과(사용자 정보, 토큰) DTO
     * @throws InvalidCredentialsException 인증 정보가 유효하지 않을 경우
     * @throws AccountLockedException    계정이 잠겨있을 경우
     */
    @Override
    public AuthResultDto signIn(SignInRequest request) {
        log.info("로그인 시도 username={}", request.username());
        // 1. 사용자 조회 및 기본 검증
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(request.username());
        } catch (UsernameNotFoundException e) {
            log.warn("로그인 실패: 존재하지 않는 사용자. username={}", request.username());
            throw new InvalidCredentialsException();
        }

        // 2. 계정 상태 검증
        if (!userDetails.isAccountNonLocked()) {
            CustomUserDetails customDetails = (CustomUserDetails) userDetails;
            log.warn("로그인 실패: 계정 잠김. userId={}", customDetails.getUserId());
            throw AccountLockedException.withId(customDetails.getUserId());
        }

        if (passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            // 3. 로그인 성공: 토큰 발급
            log.info("로그인 성공 username={}", userDetails.getUsername());
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
            log.info("로그인 성공 (임시 비밀번호) username={}", userDetails.getUsername());
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
        log.warn("로그인 실패 username={}", request.username());
        throw new InvalidCredentialsException();
    }

    /**
     * 유효한 Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 재발급한다
     * @param refreshToken 클라이언트로부터 받은 Refresh Token
     * @return 재발급된 인증 결과(사용자 정보, 토큰) DTO
     * @throws ParseException      토큰 파싱에 실패할 경우
     * @throws InvalidTokenException 토큰이 유효하지 않을 경우
     * @throws AccountLockedException 계정이 잠겨있을 경우
     */
    @Override
    public AuthResultDto reissueToken(String refreshToken) throws ParseException {
        log.info("토큰 재발급 요청");
        if (!jwtRegistry.isRefreshTokenValid(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token으로 재발급 시도");
            throw new InvalidTokenException();
        }

        // 1. Refresh Token 검증
        tokenProvider.validateRefreshToken(refreshToken);
        log.debug("Refresh Token 유효성 검증 통과");

        // 2. 토큰에서 사용자 정보 추출
        String email = tokenProvider.getEmailFromRefreshToken(refreshToken);
        log.debug("Refresh Token에서 이메일 추출 성공 email={}", email);

        // 3. DB에서 사용자 정보 조회 및 검증
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!userDetails.isAccountNonLocked()) {
            CustomUserDetails customDetails = (CustomUserDetails) userDetails;
            log.warn("토큰 재발급 실패 잠긴 계정 userId={}", customDetails.getUserId());
            throw AccountLockedException.withId(customDetails.getUserId());
        }

        // 4. 새로운 토큰들 생성
        CustomUserDetails customDetails = (CustomUserDetails) userDetails;
        UserDto userDto = customDetails.getUserDto();
        try {
            String newAccessToken = tokenProvider.createAccessToken(userDto);
            String newRefreshToken = tokenProvider.createRefreshToken(userDto);

            jwtRegistry.invalidate(refreshToken);
            log.debug("기존 Refresh Token 무효화 완료");
            jwtRegistry.register(new JwtInformation(userDto, newAccessToken, newRefreshToken));

            log.info("토큰 재발급 성공 username={}", userDetails.getUsername());
            return new AuthResultDto(userDto, newAccessToken, newRefreshToken);
        } catch (JOSEException e) {
            throw new TokenCreationException(ErrorCode.TOKEN_CREATION_FAILED);
        }
    }

    /**
     * 사용자 로그아웃 처리 및 Refresh Token을 무효화
     * @param refreshToken 무효화할 Refresh Token
     * @throws ParseException 토큰 파싱에 실패할 경우
     */
    @Override
    public void signOut(String refreshToken) throws ParseException {
        log.info("로그아웃 요청");
        tokenProvider.validateRefreshToken(refreshToken);
        jwtRegistry.invalidate(refreshToken);
        log.info("로그아웃 성공 및 토큰 무효화 완료");
    }


    /**
     * 사용자 이메일로 임시 비밀번호를 생성하여 발송한다
     * 생성된 임시 비밀번호는 해시 처리되어 3분간 Redis에 저장됨
     * @param email 임시 비밀번호를 받을 사용자의 이메일
     * @throws UserNotFoundException 해당 이메일의 사용자가 존재하지 않을 경우
     */
    @Override
    @Transactional
    public void sendTemporaryPassword(String email) {
        log.info("임시 비밀번호 발급 요청 email={}", email);
        // 1. 유저 확인
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> UserNotFoundException.withEmail(email));

        // 2. 임시 비밀번호 생성
        String tempPassword = generateRandomPassword(10);
        log.debug("임시 비밀번호 생성 완료. email={}", email);

        // 3. Redis에 해싱하여 저장
        String hashedPassword = passwordEncoder.encode(tempPassword);
        redisTemplate.opsForValue().set("temp_pw:" + email, hashedPassword, Duration.ofMinutes(3));
        log.debug("임시 비밀번호 Redis 저장 완료. email={}", email);

        // 4. MailService를 통해 메일 발송 위임
        mailService.sendTemporaryPasswordEmail(email, tempPassword);
        log.info("임시 비밀번호 이메일 발송 위임 완료 email={}", email);
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