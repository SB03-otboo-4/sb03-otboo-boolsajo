package com.sprint.otboo.notification.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.entity.Notification;
import com.sprint.otboo.notification.entity.NotificationLevel;
import com.sprint.otboo.notification.mapper.NotificationMapper;
import com.sprint.otboo.notification.repository.NotificationRepository;
import com.sprint.otboo.notification.service.impl.NotificationServiceImpl;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 테스트")
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private NotificationQueryParams notificationQuery(String cursor, UUID idAfter, int limit) {
        return new NotificationQueryParams(cursor, idAfter, limit);
    }

    private Notification notificationEntity(UUID receiverId, NotificationLevel level) {
        return Notification.builder()
            .id(UUID.randomUUID())
            .createdAt(Instant.now())
            .receiver(user(receiverId))
            .title("테스트 알림")
            .content("알림 내용")
            .level(level)
            .build();
    }

    private NotificationDto notificationDto(Notification entity) {
        return new NotificationDto(
            entity.getId(),
            entity.getCreatedAt(),
            entity.getReceiver().getId(),
            entity.getTitle(),
            entity.getContent(),
            entity.getLevel()
        );
    }

    private Slice<Notification> notificationSlice(List<Notification> content, int requestedSize, boolean hasNext) {
        PageRequest pageRequest = PageRequest.of(
            0,
            requestedSize,
            Sort.by(Direction.DESC, "createdAt")
                .and(Sort.by(Direction.DESC, "id"))
            );
        return new SliceImpl<>(content, pageRequest, hasNext);
    }


    private User user(UUID id) {
        return User.builder()
            .id(id)
            .createdAt(Instant.now())
            .username("testUser")
            .password("encodedPassword")
            .email("test@test.com")
            .role(Role.USER)
            .locked(false)
            .provider(LoginType.GENERAL)
            .build();
    }

    @Test
    void 알림_목록을_cursor_없이_조회하여_마지막_요소까지_반환() {
        // given
        UUID receiverId = UUID.randomUUID();
        NotificationQueryParams query = new NotificationQueryParams(null, null, 20);

        Notification entity = notificationEntity(receiverId, NotificationLevel.INFO);
        Slice<Notification> slice = notificationSlice(List.of(entity), query.limit(), false);
        given(notificationRepository.findByReceiverWithCursor(
            receiverId,
            query.parsedCursor(),
            query.idAfter(),
            query.fetchSize()
        )).willReturn(slice);

        NotificationDto expectedDto = notificationDto(entity);
        given(notificationMapper.toDto(entity)).willReturn(expectedDto);

        // when
        CursorPageResponse<NotificationDto> actual = notificationService.getNotifications(receiverId, query);

        // then
        assertThat(actual.data()).containsExactly(expectedDto);
        assertThat(actual.hasNext()).isFalse();
        assertThat(actual.nextCursor()).isNull();
        assertThat(actual.nextIdAfter()).isNull();
    }
}
