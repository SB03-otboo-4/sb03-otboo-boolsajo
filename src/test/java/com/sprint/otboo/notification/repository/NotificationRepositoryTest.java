package com.sprint.otboo.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.common.config.JpaAuditingConfig;
import com.sprint.otboo.common.config.QuerydslConfig;
import com.sprint.otboo.notification.entity.Notification;
import com.sprint.otboo.notification.entity.NotificationLevel;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
public class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User persistUser() {
        User user = User.builder()
            .username("testUser")
            .password("encodedPassword")
            .email("test@test.com")
            .role(Role.USER)
            .locked(false)
            .provider(LoginType.GENERAL)
            .build();

        return entityManager.persistAndFlush(user);
    }

    private Notification persistNotification(User receiver, Instant createdAt) {
        Notification notification = Notification.builder()
            .receiver(receiver)
            .title("테스트 알림")
            .content("테스트 알림입니다.")
            .level(NotificationLevel.INFO)
            .build();

        notification = entityManager.persist(notification);
        entityManager.flush();

        UUID id = notification.getId(); // UUID가 생성된 상태
        Notification managed = entityManager.find(Notification.class, id);
        ReflectionTestUtils.setField(managed, "createdAt", createdAt);
        entityManager.flush();

        return managed;
    }

    @Test
    void 커서_없이_조회하면_최신순으로_정렬() {
        // given
        User receiver = persistUser();
        Notification n1 = persistNotification(receiver, Instant.parse("2025-09-24T09:00:00Z"));
        Notification n2 = persistNotification(receiver, Instant.parse("2025-09-24T08:00:00Z"));

        // when
        Slice<Notification> slice = notificationRepository.findByReceiverWithCursor(
            receiver.getId(),
            null,
            null,
            3
        );

        // then
        assertThat(slice.getContent()).extracting(Notification::getId).containsExactly(n1.getId(), n2.getId());
        assertThat(slice.hasNext()).isFalse();
    }

    @Test
    void 커서를_이용해_이후_데이터만_조회() {
        // given
        User receiver = persistUser();
        Notification newer = persistNotification(receiver, Instant.parse("2025-09-24T10:00:00Z"));
        Notification older = persistNotification(receiver, Instant.parse("2025-09-24T09:00:00Z"));

        // when
        Slice<Notification> slice = notificationRepository.findByReceiverWithCursor(
            receiver.getId(),
            newer.getCreatedAt(),
            newer.getId(),
            3
        );

        // then
        assertThat(slice.getContent()).extracting(Notification::getId).containsExactly(older.getId());
    }
}
