package com.sprint.otboo.auth.jwt;

import com.sprint.otboo.auth.dto.JwtInformation;

public interface JwtRegistry {

    /**
     * 새로운 JWT 세션 정보를 등록
     * 최대 활성 세션 개수를 초과하면 가장 오래된 세션을 자동으로 무효화
     */
    void register(JwtInformation jwtInformation);

    /**
     * 전달된 Access Token이 현재 유효한 세션인지 확인
     */
    boolean isAccessTokenValid(String accessToken);

    /**
     * 전달된 Refresh Token이 현재 유효한 세션인지 확인
     */
    boolean isRefreshTokenValid(String refreshToken);

    /**
     * 전달된 Refresh Token에 해당하는 세션을 무효화(로그아웃)
     */
    void invalidate(String refreshToken);
}
