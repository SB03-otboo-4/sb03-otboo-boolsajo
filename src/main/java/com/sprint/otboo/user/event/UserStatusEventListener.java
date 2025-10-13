package com.sprint.otboo.user.event;

import com.sprint.otboo.auth.jwt.JwtRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatusEventListener {

    private final JwtRegistry jwtRegistry;

    /**
     * 계정이 잠금 상태로 전환되면 모든 JWT 토큰을 강제로 만료시킨다.
     *
     * @param event 잠금된 사용자 ID를 포함한 모든 이벤트
     * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserLockedEvent(UserLockedEvent event) {
        log.info("계정 잠금 이벤트 수신. 강제 로그아웃 처리: userId={}", event.userId());
        jwtRegistry.invalidateAll(event.userId());
    }

    /**
     * 권한이 변경된 사용자의 기존 세션을 무효화
     * 권한 변경 후 재 로그인을 요구하기 위한 처리
     * @param event 이전/새 Role과 사용자 ID를 담은 이벤트
     * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRoleChangedEvent(UserRoleChangedEvent event) {
        log.info("권한 변경 이벤트 수신. 강제 로그아웃 처리: userId={}", event.userId());
        jwtRegistry.invalidateAll(event.userId());
    }
}
