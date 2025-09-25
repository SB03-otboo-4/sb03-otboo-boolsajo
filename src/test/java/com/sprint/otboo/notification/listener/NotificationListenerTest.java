package com.sprint.otboo.notification.listener;

import static org.mockito.BDDMockito.then;

import com.sprint.otboo.notification.service.NotificationService;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.event.UserRoleChangedEvent;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationListener 테스트")
public class NotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationListener notificationListener;

    @Test
    void 권한_변경_이벤트를_받으면_알림_생성을_위임() {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleChangedEvent event = new UserRoleChangedEvent(userId, Role.USER, Role.ADMIN);

        // when
        notificationListener.handleUserRoleChanged(event);

        // then
        then(notificationService).should().notifyRoleChanged(userId, Role.ADMIN);
    }
}
