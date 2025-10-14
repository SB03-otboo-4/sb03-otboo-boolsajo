package com.sprint.otboo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
import com.sprint.otboo.notification.dto.response.NotificationCursorResponse;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.entity.Notification;
import com.sprint.otboo.notification.entity.NotificationLevel;
import com.sprint.otboo.notification.mapper.NotificationMapper;
import com.sprint.otboo.notification.repository.NotificationRepository;
import com.sprint.otboo.notification.service.impl.NotificationServiceImpl;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
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
    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationSseService notificationSseService;

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

    private Notification notificationEntityOwnedBy(User receiver, NotificationLevel level) {
        return Notification.builder()
            .id(UUID.randomUUID())
            .createdAt(Instant.now())
            .receiver(receiver)
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
            receiverId, query.parsedCursor(), query.idAfter(), query.fetchSize()
        )).willReturn(slice);
        NotificationDto dto = notificationDto(entity);
        given(notificationMapper.toDto(entity)).willReturn(dto);
        given(notificationRepository.countByReceiverId(receiverId)).willReturn(5L);

        // when
        NotificationCursorResponse actual =
            notificationService.getNotifications(receiverId, query);

        // then
        assertThat(actual.data()).containsExactly(dto);
        assertThat(actual.hasNext()).isFalse();
        assertThat(actual.totalCount()).isEqualTo(5L);
        assertThat(actual.cursor()).isNull();
        assertThat(actual.idAfter()).isNull();
    }

    @Test
    void 알림_목록을_조회하면_hasNext와_cursor_정보를_갱신() {
        // given
        UUID receiverId = UUID.randomUUID();
        NotificationQueryParams query = new NotificationQueryParams(null, null, 2);

        Notification first = notificationEntity(receiverId, NotificationLevel.INFO);
        Notification second = notificationEntity(receiverId, NotificationLevel.WARNING);

        Slice<Notification> slice = new SliceImpl<>(
            List.of(first, second),
            PageRequest.of(0, query.fetchSize() - 1),
            true
        );
        NotificationDto firstDto = notificationDto(first);
        NotificationDto secondDto = notificationDto(second);

        given(notificationRepository.findByReceiverWithCursor(
            receiverId, query.parsedCursor(), query.idAfter(), query.fetchSize()
        )).willReturn(slice);
        given(notificationMapper.toDto(first)).willReturn(firstDto);
        given(notificationMapper.toDto(second)).willReturn(secondDto);
        given(notificationRepository.countByReceiverId(receiverId)).willReturn(42L);

        // when
        NotificationCursorResponse actual =
            notificationService.getNotifications(receiverId, query);

        // then
        assertThat(actual.data()).containsExactly(firstDto, secondDto);
        assertThat(actual.hasNext()).isTrue();
        NotificationDto last = actual.data().get(actual.data().size() - 1);
        Instant expectedCursor = last.createdAt()
            .truncatedTo(ChronoUnit.MILLIS);
        assertThat(actual.cursor()).isEqualTo(expectedCursor.toString());
        assertThat(actual.idAfter()).isEqualTo(last.id().toString());
        assertThat(actual.totalCount()).isEqualTo(42L);
    }

    @Test
    void 권한_변경_알림을_저장하고_DTO로_반환() {
        // given
        UUID receiverId = UUID.randomUUID();
        Role newRole = Role.ADMIN;
        User receiver = user(receiverId);

        given(userRepository.getReferenceById(receiverId)).willReturn(receiver);

        Notification saved = notificationEntityOwnedBy(receiver, NotificationLevel.INFO);
        NotificationDto expectedDto = notificationDto(saved);

        given(notificationRepository.saveAndFlush(any(Notification.class))).willReturn(saved);
        given(notificationMapper.toDto(saved)).willReturn(expectedDto);

        // when
        NotificationDto result = notificationService.notifyRoleChanged(receiverId, newRole);

        // then
        then(notificationRepository).should().saveAndFlush(any(Notification.class));
        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void 의상_속성_추가_알림을_모든_사용자에게_저장() {
        // given
        String attributeName = "기능성";
        User user1 = user(UUID.randomUUID());
        User user2 = user(UUID.randomUUID());

        given(userRepository.findAll()).willReturn(List.of(user1, user2));

        Notification saved1 = notificationEntityOwnedBy(user1, NotificationLevel.INFO);
        Notification saved2 = notificationEntityOwnedBy(user2, NotificationLevel.INFO);

        given(notificationRepository.saveAndFlush(any(Notification.class)))
            .willReturn(saved1)
            .willReturn(saved2);

        // sendToRole로 맞춤
        doNothing().when(notificationSseService).sendToRole(any(), any());

        // when
        notificationService.notifyClothesAttributeCreatedForAllUsers(attributeName);

        // then
        then(notificationRepository).should(times(2)).saveAndFlush(any(Notification.class));
        then(notificationSseService).should(times(2)).sendToRole(any(), any());
    }


    @Test
    void 피드_좋아요_알림을_저장하고_DTO를_반환() {
        // given
        UUID authorId = UUID.randomUUID();
        UUID likedById = UUID.randomUUID();
        User author = user(authorId);
        User liker = user(likedById);
        Notification saved = notificationEntityOwnedBy(author, NotificationLevel.INFO);
        NotificationDto expected = notificationDto(saved);

        given(userRepository.getReferenceById(authorId)).willReturn(author);
        given(userRepository.getReferenceById(likedById)).willReturn(liker);
        given(notificationRepository.saveAndFlush(any(Notification.class))).willReturn(saved);
        given(notificationMapper.toDto(saved)).willReturn(expected);

        // when
        NotificationDto result = notificationService.notifyFeedLiked(authorId, likedById);

        // then
        assertThat(result).isEqualTo(expected);
        then(notificationRepository).should().saveAndFlush(any(Notification.class));
    }

    @Test
    void 피드_댓글_알림을_저장하고_DTO를_반환() {
        // given
        UUID authorId = UUID.randomUUID();
        UUID commenterId = UUID.randomUUID();
        User author = user(authorId);
        User commenter = user(commenterId);
        Notification saved = notificationEntityOwnedBy(author, NotificationLevel.INFO);
        NotificationDto expected = notificationDto(saved);

        given(userRepository.getReferenceById(authorId)).willReturn(author);
        given(userRepository.getReferenceById(commenterId)).willReturn(commenter);
        given(notificationRepository.saveAndFlush(any(Notification.class))).willReturn(saved);
        given(notificationMapper.toDto(saved)).willReturn(expected);

        // when
        NotificationDto result = notificationService.notifyFeedCommented(authorId, commenterId);

        // then
        assertThat(result).isEqualTo(expected);
        then(notificationRepository).should().saveAndFlush(any(Notification.class));
    }

    @Test
    void 알림_삭제_시_Repository에서_제거() {
        // given
        UUID notificationId = UUID.randomUUID();
        given(notificationRepository.existsById(notificationId)).willReturn(true);

        // when
        notificationService.deleteNotification(notificationId);

        // then
        then(notificationRepository).should().existsById(notificationId);
        then(notificationRepository).should().deleteById(notificationId);
    }

    @Test
    void 존재하지_않는_알림_삭제_시_예외_발생() {
        // given
        UUID notificationId = UUID.randomUUID();
        given(notificationRepository.existsById(notificationId)).willReturn(false);

        // when
        Throwable thrown = catchThrowable(() ->
            notificationService.deleteNotification(notificationId)
        );

        // then
        assertThat(thrown)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
}
