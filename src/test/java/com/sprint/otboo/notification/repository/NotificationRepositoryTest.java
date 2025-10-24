package com.sprint.otboo.notification.repository;

import static com.sprint.otboo.notification.entity.NotificationLevel.INFO;
import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.common.config.JpaAuditingConfig;
import com.sprint.otboo.common.config.QuerydslConfig;
import com.sprint.otboo.notification.entity.Notification;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import java.time.Instant;
import java.util.List;
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
            .level(INFO)
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

    @Test
    void 다른사용자_알림은_조회되지_않는지() {
        // given: 서로 다른 두 사용자와 각자의 알림을 저장
        User receiver1 = User.builder()
            .username("user1_" + UUID.randomUUID())
            .email("user1_" + UUID.randomUUID() + "@test.com")
            .password("pw")
            .role(Role.USER)
            .locked(false)
            .provider(LoginType.GENERAL)
            .build();
        receiver1 = entityManager.persistAndFlush(receiver1);

        User receiver2 = User.builder()
            .username("user2_" + UUID.randomUUID())
            .email("user2_" + UUID.randomUUID() + "@test.com")
            .password("pw")
            .role(Role.USER)
            .locked(false)
            .provider(LoginType.GENERAL)
            .build();
        receiver2 = entityManager.persistAndFlush(receiver2);

        Notification n1 = Notification.builder()
            .receiver(receiver1)
            .title("알림1")
            .content("내용1")
            .level(INFO)
            .build();
        n1 = entityManager.persistAndFlush(n1);

        Notification n2 = Notification.builder()
            .receiver(receiver2)
            .title("알림2")
            .content("내용2")
            .level(INFO)
            .build();
        n2 = entityManager.persistAndFlush(n2);

        // when: receiver1 알림만 조회
        List<Notification> result = notificationRepository.findByReceiverIdAndIdAfter(
            receiver1.getId(),
            null
        );

        // then: receiver1 알림만 조회됨을 검증
        assertThat(result).extracting(Notification::getId).containsExactly(n1.getId());
    }

    @Test
    void lastId_null이면_모든_알림_조회() {
        // given: 사용자와 알림 2개
        User receiver = entityManager.persistAndFlush(User.builder()
            .username("user_" + UUID.randomUUID())
            .email("user@test.com")
            .password("pw").role(Role.USER).locked(false).provider(LoginType.GENERAL).build());

        Notification n1 = entityManager.persistAndFlush(Notification.builder().receiver(receiver).title("1").content("A").level(INFO).build());
        Notification n2 = entityManager.persistAndFlush(Notification.builder().receiver(receiver).title("2").content("B").level(INFO).build());

        // when: lastId가 null인 상태로 알림 조회
        List<Notification> result = notificationRepository.findByReceiverIdAndIdAfter(receiver.getId(), null);

        // then: 모든 알림이 조회됨
        assertThat(result).extracting(Notification::getId).containsExactlyInAnyOrder(n1.getId(), n2.getId());
    }

    @Test
    void 알림_없는_사용자_조회시_빈리스트() {
        // given: 알림 없는 사용자
        User receiver = entityManager.persistAndFlush(User.builder()
            .username("empty_user")
            .email("empty@test.com")
            .password("pw").role(Role.USER).locked(false).provider(LoginType.GENERAL).build());

        // when: 알림 조회
        List<Notification> result = notificationRepository.findByReceiverIdAndIdAfter(receiver.getId(), null);

        // then: 조회 결과가 빈 리스트임을 검증
        assertThat(result).isEmpty();
    }
}
