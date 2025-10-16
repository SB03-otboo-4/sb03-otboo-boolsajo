package com.sprint.otboo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.follow.repository.FollowRepository;
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
import java.util.Optional;
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
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationSseService notificationSseService;
    @Mock
    private FollowRepository followRepository;

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

        // saveAndFlush가 연속 호출될 때 순서대로 반환되도록 설정
        given(notificationRepository.saveAndFlush(any(Notification.class)))
            .willReturn(saved1, saved2);

        // 서비스는 DB에 저장된 DTO(-> mapper)를 만들어 sendToClient 호출
        NotificationDto dto1 = notificationDto(saved1);
        NotificationDto dto2 = notificationDto(saved2);
        given(notificationMapper.toDto(saved1)).willReturn(dto1);
        given(notificationMapper.toDto(saved2)).willReturn(dto2);

        // notificationSseService.sendToClient 호출은 아무 동작 없이 통과
        doNothing().when(notificationSseService).sendToClient(any(NotificationDto.class));

        // when
        notificationService.notifyClothesAttributeCreatedForAllUsers(attributeName);

        // then
        then(notificationRepository).should(times(2)).saveAndFlush(any(Notification.class));
        // sendToClient가 사용자 수만큼 호출되는지 검증
        then(notificationSseService).should(times(2)).sendToClient(any(NotificationDto.class));
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
        User owner = user(UUID.randomUUID());
        Notification existing = Notification.builder()
            .id(notificationId)
            .createdAt(Instant.now())
            .receiver(owner)
            .title("삭제 대상")
            .content("내용")
            .level(NotificationLevel.INFO)
            .build();

        NotificationDto dto = new NotificationDto(
            existing.getId(),
            existing.getCreatedAt(),
            existing.getReceiver().getId(),
            existing.getTitle(),
            existing.getContent(),
            existing.getLevel()
        );

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(existing));
        given(notificationMapper.toDto(existing)).willReturn(dto);

        // sendToClient 정상 동작하도록 설정
        doNothing().when(notificationSseService).sendToClient(dto);

        // when
        notificationService.deleteNotification(notificationId);

        // then
        then(notificationRepository).should().findById(notificationId);
        then(notificationSseService).should().sendToClient(dto);
        then(notificationRepository).should().deleteById(notificationId);
    }

    @Test
    void 존재하지_않는_알림_삭제_시_예외_발생() {
        // given
        UUID notificationId = UUID.randomUUID();
        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() ->
            notificationService.deleteNotification(notificationId)
        );

        // then
        assertThat(thrown)
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        // findById가 호출되었음을 체크
        then(notificationRepository).should().findById(notificationId);
        // deleteById는 호출되지 않아야 함
        then(notificationRepository).should(never()).deleteById(any());
    }

    @Test
    void lastEventId_null이면_빈_리스트_반환() {
        // given: receiverId와 lastEventId가 null
        UUID receiverId = UUID.randomUUID();

        // when: 누락 알림 조회 실행
        List<NotificationDto> result = notificationService.getMissedNotifications(receiverId, null);

        // then: 결과가 빈 리스트임을 검증
        assertThat(result).isEmpty();
    }

    @Test
    void lastEventId_잘못된_형식이면_빈_리스트_반환() {
        // given: receiverId와 잘못된 lastEventId
        UUID receiverId = UUID.randomUUID();
        String invalidId = "not-a-uuid";

        // when: 누락 알림 조회 실행
        List<NotificationDto> result = notificationService.getMissedNotifications(receiverId, invalidId);

        // then: 결과가 빈 리스트임을 검증
        assertThat(result).isEmpty();
    }

    @Test
    void 누락_알림_조회_및_Dto_변환() {
        // given: receiverId, 누락 알림 2개, 마지막 수신 이벤트 ID
        UUID receiverId = UUID.randomUUID();
        Notification n1 = notificationEntity(receiverId, NotificationLevel.INFO);
        Notification n2 = notificationEntity(receiverId, NotificationLevel.INFO);
        String lastEventId = UUID.randomUUID().toString();

        given(notificationRepository.findByReceiverIdAndIdAfter(receiverId, UUID.fromString(lastEventId)))
            .willReturn(List.of(n1, n2));
        given(notificationMapper.toDto(n1)).willReturn(notificationDto(n1));
        given(notificationMapper.toDto(n2)).willReturn(notificationDto(n2));

        // when: 누락 알림 조회 실행
        List<NotificationDto> result = notificationService.getMissedNotifications(receiverId, lastEventId);

        // then: 결과가 2건이며 ID 순서가 예상과 일치함을 검증
        assertThat(result).hasSize(2);
        assertThat(result).extracting(NotificationDto::id)
            .containsExactly(n1.getId(), n2.getId());
    }

    @Test
    void repository에서_누락_알림_없으면_빈_리스트_반환() {
        // given: receiverId와 마지막 수신 이벤트 ID, repository에서 누락 알림이 없도록 설정
        UUID receiverId = UUID.randomUUID();
        String lastEventId = UUID.randomUUID().toString();

        given(notificationRepository.findByReceiverIdAndIdAfter(receiverId, UUID.fromString(lastEventId)))
            .willReturn(List.of());

        // when: 누락 알림 조회 실행
        List<NotificationDto> result = notificationService.getMissedNotifications(receiverId, lastEventId);

        // then: 결과가 빈 리스트임을 검증
        assertThat(result).isEmpty();
    }

    @Test
    void 알림_삭제_중_SSE_전송_실패시_DB_삭제_되지_않음() {
        // given: 존재하는 알림과 매핑된 DTO, 전송 실패 설정
        UUID notificationId = UUID.randomUUID();
        User receiver = user(UUID.randomUUID());
        Notification existing = Notification.builder()
            .id(notificationId)
            .createdAt(Instant.now())
            .receiver(receiver)
            .title("삭제 테스트")
            .content("전송 실패 케이스")
            .level(NotificationLevel.INFO)
            .build();

        NotificationDto dto = notificationDto(existing);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(existing));
        given(notificationMapper.toDto(existing)).willReturn(dto);

        // sendToClient가 예외 발생시키도록 설정
        willThrow(new RuntimeException("전송 실패"))
            .given(notificationSseService).sendToClient(dto);

        // when: 알림 삭제 요청 실행
        notificationService.deleteNotification(notificationId);

        // then: DB 삭제는 수행되지 않아야 함
        then(notificationRepository).should().findById(notificationId);
        then(notificationSseService).should().sendToClient(dto);
        then(notificationRepository).should(never()).deleteById(notificationId);
    }

    @Test
    void 의상_속성_추가시_사용자_없으면_전송_스킵() {
        // given: 사용자 목록이 비어 있음
        String attributeName = "신규 속성";
        given(userRepository.findAll()).willReturn(List.of());

        // when: 모든 사용자에게 속성 생성 알림 브로드캐스트 실행
        notificationService.notifyClothesAttributeCreatedForAllUsers(attributeName);

        // then: DB 저장 및 SSE 전송이 수행되지 않음
        then(notificationRepository).should(never()).saveAndFlush(any());
        then(notificationSseService).should(never()).sendToClient(any());
    }

    @Test
    void 팔로워가_없으면_피드_알림을_미생성() {
        // given
        UUID authorId = UUID.randomUUID();
        given(followRepository.findFollowerIdsByFolloweeId(authorId)).willReturn(List.of());

        // when
        notificationService.notifyFollowersFeedCreated(authorId, UUID.randomUUID());

        // then
        then(notificationRepository).shouldHaveNoInteractions();
        then(notificationMapper).shouldHaveNoInteractions();
        then(notificationSseService).shouldHaveNoInteractions();
    }

    @Test
    void 팔로워들에게_피드_알림을_저장하고_SSE로_전송() {
        // given
        UUID authorId = UUID.randomUUID();
        UUID follower1Id = UUID.randomUUID();
        UUID follower2Id = UUID.randomUUID();

        User author = user(authorId);
        User follower1 = user(follower1Id);
        User follower2 = user(follower2Id);

        Notification saved1 = notificationEntityOwnedBy(follower1, NotificationLevel.INFO);
        Notification saved2 = notificationEntityOwnedBy(follower2, NotificationLevel.INFO);
        NotificationDto dto1 = notificationDto(saved1);
        NotificationDto dto2 = notificationDto(saved2);

        given(followRepository.findFollowerIdsByFolloweeId(authorId))
            .willReturn(List.of(follower1Id, follower2Id));
        given(userRepository.getReferenceById(authorId)).willReturn(author);
        given(userRepository.getReferenceById(follower1Id)).willReturn(follower1);
        given(userRepository.getReferenceById(follower2Id)).willReturn(follower2);
        given(notificationRepository.saveAndFlush(any(Notification.class)))
            .willReturn(saved1, saved2);
        given(notificationMapper.toDto(saved1)).willReturn(dto1);
        given(notificationMapper.toDto(saved2)).willReturn(dto2);

        // when
        notificationService.notifyFollowersFeedCreated(authorId, UUID.randomUUID());

        // then
        then(notificationRepository).should(times(2)).saveAndFlush(any(Notification.class));
        then(notificationMapper).should(times(2)).toDto(any(Notification.class));
        then(notificationSseService).should().sendToClient(dto1);
        then(notificationSseService).should().sendToClient(dto2);
    }

    @Test
    void 새_팔로워_알림을_저장하고_SSE로_전송() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();

        User follower = user(followerId);
        User followee = user(followeeId);

        Notification saved = notificationEntityOwnedBy(followee, NotificationLevel.INFO);
        NotificationDto dto = notificationDto(saved);

        given(userRepository.getReferenceById(followerId)).willReturn(follower);
        given(userRepository.getReferenceById(followeeId)).willReturn(followee);
        given(notificationRepository.saveAndFlush(any(Notification.class))).willReturn(saved);
        given(notificationMapper.toDto(saved)).willReturn(dto);

        // when
        NotificationDto result = notificationService.notifyUserFollowed(followerId, followeeId);

        // then
        assertThat(result).isEqualTo(dto);
        then(notificationRepository).should().saveAndFlush(any(Notification.class));
        then(notificationMapper).should().toDto(saved);
        then(notificationSseService).should().sendToClient(dto);
    }
}
