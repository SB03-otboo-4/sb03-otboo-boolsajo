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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserLockedEvent(UserLockedEvent event) {
        log.info("계정 잠금 이벤트 수신. 강제 로그아웃 처리: userId={}", event.userId());
        jwtRegistry.invalidateAll(event.userId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRoleChangedEvent(UserRoleChangedEvent event) {
        log.info("권한 변경 이벤트 수신. 강제 로그아웃 처리: userId={}", event.userId());
        jwtRegistry.invalidateAll(event.userId());
    }
}
