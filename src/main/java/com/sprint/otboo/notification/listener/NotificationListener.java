package com.sprint.otboo.notification.listener;

import com.sprint.otboo.notification.service.NotificationService;
import com.sprint.otboo.user.event.UserRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRoleChanged(UserRoleChangedEvent event) {
        log.debug("[NotificationListener] handleUserRoleChanged: userId={}, newRole={}",
            event.userId(), event.newRole());

        notificationService.notifyRoleChanged(event.userId(), event.newRole());
    }
}
