package com.sprint.otboo.notification.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.entity.Notification;
import com.sprint.otboo.notification.entity.NotificationLevel;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class NotificationMapperTest {

    private final NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    void 알림_엔티티_DTO로_매핑() {
        // given
        UUID receiverId = UUID.randomUUID();
        User receiver = User.builder()
            .id(receiverId)
            .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
            .username("testUser")
            .password("encodedPassword")
            .email("test@test.com")
            .role(Role.USER)
            .locked(false)
            .provider(LoginType.GENERAL)
            .build();

        Instant createdAt = Instant.parse("2025-02-02T00:00:00Z");
        UUID notificationId = UUID.randomUUID();
        Notification entity = Notification.builder()
            .id(notificationId)
            .createdAt(createdAt)
            .receiver(receiver)
            .title("test 알림")
            .content("테스트 알림입니다.")
            .level(NotificationLevel.INFO)
            .build();

        // when
        NotificationDto dto = mapper.toDto(entity);

        // then
        assertThat(dto.id()).isEqualTo(notificationId);
        assertThat(dto.createdAt()).isEqualTo(createdAt);
        assertThat(dto.receiverId()).isEqualTo(receiverId);
        assertThat(dto.title()).isEqualTo("test 알림");
        assertThat(dto.content()).isEqualTo("테스트 알림입니다.");
        assertThat(dto.level()).isEqualTo(NotificationLevel.INFO);
    }

}
