package com.sprint.otboo.user.service.support;

import static org.mockito.BDDMockito.then;

import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.event.UserLockedEvent;
import com.sprint.otboo.user.event.UserRoleChangedEvent;
import com.sprint.otboo.user.event.UserStatusEventListener;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserStatusEventListenerTest {

    @Mock
    private JwtRegistry jwtRegistry;

    @InjectMocks
    private UserStatusEventListener userStatusEventListener;

    @Test
    void 계정_잠금_이벤트_처리_테스트() {
        // given
        UUID userId = UUID.randomUUID();
        UserLockedEvent event = new UserLockedEvent(userId);

        // when
        userStatusEventListener.handleUserLockedEvent(event);

        // then
        then(jwtRegistry).should().invalidateAll(userId);
    }

    @Test
    void 권한_변경_이벤트_처리_테스트() {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleChangedEvent event = new UserRoleChangedEvent(userId, Role.USER, Role.ADMIN);

        // when
        userStatusEventListener.handleUserRoleChangedEvent(event);

        // then
        then(jwtRegistry).should().invalidateAll(userId);
    }
}
