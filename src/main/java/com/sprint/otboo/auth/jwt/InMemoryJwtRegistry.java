package com.sprint.otboo.auth.jwt;

import com.sprint.otboo.auth.dto.JwtInformation;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InMemoryJwtRegistry implements JwtRegistry {

    private final Map<UUID, Queue<JwtInformation>> activeSessions = new ConcurrentHashMap<>();
    private final Set<String> validAccessTokens = ConcurrentHashMap.newKeySet();
    private final Set<String> validRefreshTokens = ConcurrentHashMap.newKeySet();

    // RefreshToken을 통해 JwtInformation을 빠르게 찾기 위한 인덱스
    private final Map<String, JwtInformation> refreshTokenIndex = new ConcurrentHashMap<>();

    private final int maxActiveJwtCount;

    public InMemoryJwtRegistry(@Value("${jwt.session.max-active-jwt}") int maxActiveJwtCount) {
        this.maxActiveJwtCount = maxActiveJwtCount;
    }

    @Override
    public void register(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.userDto().id();

        // 1. 사용자의 세션 큐 가져오기 (없으면 생성)
        Queue<JwtInformation> sessionQueue = activeSessions.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());

        // 2. 최대 세션 수 초과 시 가장 오래된 세션 제거 (강제 로그아웃)
        while (sessionQueue.size() >= maxActiveJwtCount) {
            JwtInformation oldestSession = sessionQueue.poll(); // 가장 오래된 세션 꺼내기
            if (oldestSession != null) {
                invalidateTokens(oldestSession);
            }
        }

        // 3. 새 세션 추가
        sessionQueue.add(jwtInformation);
        validAccessTokens.add(jwtInformation.accessToken());
        validRefreshTokens.add(jwtInformation.refreshToken());
        refreshTokenIndex.put(jwtInformation.refreshToken(), jwtInformation);
    }

    @Override
    public boolean isAccessTokenValid(String accessToken) {
        return validAccessTokens.contains(accessToken);
    }

    @Override
    public boolean isRefreshTokenValid(String refreshToken) {
        return validRefreshTokens.contains(refreshToken);
    }

    @Override
    public void invalidate(String refreshToken) {
        JwtInformation sessionInfo = refreshTokenIndex.get(refreshToken);
        if (sessionInfo != null) {
            // 세션 큐에서도 제거
            Queue<JwtInformation> sessionQueue = activeSessions.get(sessionInfo.userDto().id());
            if (sessionQueue != null) {
                sessionQueue.remove(sessionInfo);
            }
            // 모든 인덱스에서 토큰 정보 제거
            invalidateTokens(sessionInfo);
        }
    }

    private void invalidateTokens(JwtInformation sessionInfo) {
        validAccessTokens.remove(sessionInfo.accessToken());
        validRefreshTokens.remove(sessionInfo.refreshToken());
        refreshTokenIndex.remove(sessionInfo.refreshToken());
    }
}
