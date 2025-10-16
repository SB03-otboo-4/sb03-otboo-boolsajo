package com.sprint.otboo.notification.service.impl;

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
import com.sprint.otboo.notification.service.NotificationService;
import com.sprint.otboo.notification.service.NotificationSseService;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final NotificationSseService notificationSseService;

    /**
     * 알림을 조회해 DTO로 변환하고, 다음 페이지 진입을 위한 커서를 계산
     *
     * @param receiverId 알림 수신자
     * @param query 커서/아이디/limit 정보
     * @return 다음 커서·아이디 포함 응답 DTO
     * */
    @Override
    @Transactional(readOnly = true)
    public NotificationCursorResponse getNotifications(UUID receiverId, NotificationQueryParams query) {
        log.debug("[NotificationServiceImpl] 알림 조회 시작 : 사용자 = {}, cursor = {}, idAfter = {}, fetchSize = {}",
            receiverId, query.cursor(), query.idAfter(), query.fetchSize());

        Instant cursorInstant = query.parsedCursor();
        UUID idAfter = query.idAfter();

        var slice = notificationRepository.findByReceiverWithCursor(
            receiverId,
            cursorInstant,
            idAfter,
            query.fetchSize()
        );

        log.debug("[NotificationServiceImpl] 알림 조회 완료 : 조회수 = {}, 다음 페이지 여부 = {}",
            slice.getNumberOfElements(), slice.hasNext());

        List<NotificationDto> data = slice.getContent()
            .stream()
            .map(notificationMapper::toDto)
            .toList();

        NotificationDto last = !data.isEmpty()
            ? data.get(data.size() - 1)
            : null;

        String nextCursor = (last != null && slice.hasNext())
            ? last.createdAt().truncatedTo(ChronoUnit.MILLIS).toString()
            : null;

        String nextIdAfter = (last != null && slice.hasNext())
            ? last.id().toString()
            : null;

        long totalCount = notificationRepository.countByReceiverId(receiverId);

        log.debug("[NotificationServiceImpl] 다음 커서 정보 : cursor = {}, idAfter = {}, hasNext = {}",
            nextCursor, nextIdAfter, slice.hasNext());

        return NotificationCursorResponse.from(
            data,
            nextCursor,
            nextIdAfter,
            slice.hasNext(),
            totalCount,
            "createdAt",
            "DESCENDING"
        );
    }

    /**
     * 권한 변경 알림을 별도 트랜잭션에서 생성해 롤백 전파를 방지
     *
     * @param receiverId 알림 수신자
     * @param newRole 새 권한
     * @return  저장된 알림 DTO
     * */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDto notifyRoleChanged(UUID receiverId, Role newRole) {
        log.info("[NotificationServiceImpl] 권한 변경 알림 생성 : 사용자 = {}, 새로운 권한 = {}", receiverId, newRole);

        User receiver = userRepository.getReferenceById(receiverId);
        log.debug("[NotificationService] 사용자 조회 완료 : {}", receiver.getId());

        Notification notification = Notification.builder()
            .receiver(receiver)
            .title("권한이 변경 되었습니다.")
            .content("변경된 권한은 %s 입니다.".formatted(newRole.name()))
            .level(NotificationLevel.INFO)
            .build();

        NotificationDto dto = saveAndMap(notification);
        log.debug("[NotificationService] 알림 저장 완료 : {}", dto.id());

        // 권한 변경 알림은 개별 사용자에게만 전송
        notificationSseService.sendToClient(dto);
        log.info("[NotificationService] SSE 전송 완료 : 사용자={}", receiverId);

        return dto;
    }

    /**
     * 모든 사용자에게 새 의상 속성 알림을 브로드캐스트하고 DB에 저장합니다.
     *
     * <ul>
     *     <li>각 사용자별 Notification 엔티티를 DB에 저장</li>
     *     <li>저장된 NotificationDto를 사용하여 SSE로 실시간 전송</li>
     *     <li>삭제 기능 시 DB ID와 일치하여 처리 가능</li>
     * </ul>
     *
     * @param attributeName 새로 생성된 의상 속성 이름
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyClothesAttributeCreatedForAllUsers(String attributeName) {
        log.info("[NotificationService] 새 의상 속성 알림 브로드캐스트 시작 : {}", attributeName);

        List<User> receivers = userRepository.findAll(); // ADMIN, USER 역할 필터 가능
        log.debug("[NotificationService] 알림 대상 사용자 수={}", receivers.size());

        for (User receiver : receivers) {
            // DB에 Notification 저장
            Notification notification = Notification.builder()
                .receiver(receiver)
                .title("새 의상 속성이 등록되었습니다.")
                .content("내 의상에 [%s] 속성을 추가해보세요.".formatted(attributeName))
                .level(NotificationLevel.INFO)
                .build();

            NotificationDto savedDto = saveAndMap(notification);

            // 실제 DB ID 가진 DTO로 SSE 전송
            notificationSseService.sendToClient(savedDto);
        }

        log.info("[NotificationService] 모든 권한자에게 SSE 브로드캐스트 완료");
    }

    /**
     * 피드 좋아요 발생 시 알림 생성 및 전송
     *
     * <ul>
     *     <li>좋아요한 사용자와 피드 작성자를 기반으로 Notification 생성</li>
     *     <li>DB 저장 후 NotificationDto 반환</li>
     *     <li>개별 사용자 SSE 전송</li>
     * </ul>
     *
     * @param feedAuthorId 피드 작성자 UUID
     * @param likedByUserId 좋아요한 사용자 UUID
     * @return 생성된 NotificationDto
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDto notifyFeedLiked(UUID feedAuthorId, UUID likedByUserId) {
        log.info("[NotificationServiceImpl] 피드 좋아요 알림 생성: 작성자 = {}, 좋아요사용자 = {}", feedAuthorId, likedByUserId);

        User receiver = userRepository.getReferenceById(feedAuthorId);
        User liker = userRepository.getReferenceById(likedByUserId);

        Notification notification = Notification.builder()
            .receiver(receiver)
            .title("피드에 새로운 좋아요")
            .content("%s 님이 피드를 좋아합니다.".formatted(liker.getUsername()))
            .level(NotificationLevel.INFO)
            .build();

        NotificationDto dto = saveAndMap(notification);
        log.debug("[NotificationService] 알림 저장 완료 : {}", dto.id());

        // 개인 대상 전송
        notificationSseService.sendToClient(dto);
        log.info("[NotificationService] 개인 SSE 전송 완료 : 사용자={}", feedAuthorId);

        return dto;
    }

    /**
     * 피드 댓글 발생 시 알림 생성 및 전송
     *
     * <ul>
     *     <li>댓글 작성자와 피드 작성자를 기반으로 Notification 생성</li>
     *     <li>DB 저장 후 NotificationDto 반환</li>
     *     <li>개별 사용자 SSE 전송</li>
     * </ul>
     *
     * @param feedAuthorId 피드 작성자 UUID
     * @param commentedByUserId 댓글 작성자 UUID
     * @return 생성된 NotificationDto
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDto notifyFeedCommented(UUID feedAuthorId, UUID commentedByUserId) {
        log.info("[NotificationServiceImpl] 피드 댓글 알림 생성 : 작성자 = {}, 댓글사용자 = {}", feedAuthorId, commentedByUserId);

        User receiver = userRepository.getReferenceById(feedAuthorId);
        User commenter = userRepository.getReferenceById(commentedByUserId);

        Notification notification = Notification.builder()
            .receiver(receiver)
            .title("피드에 새로운 댓글")
            .content("%s 님이 댓글을 남겼습니다.".formatted(commenter.getUsername()))
            .level(NotificationLevel.INFO)
            .build();

        NotificationDto dto = saveAndMap(notification);
        log.debug("[NotificationService] 알림 저장 완료 : {}", dto.id());

        // 개인 대상 전송
        notificationSseService.sendToClient(dto);
        log.info("[NotificationService] 개인 SSE 전송 완료 : 사용자={}", feedAuthorId);

        return dto;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyFollowersFeedCreated(UUID feedAuthorId, UUID feedId) {
        List<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(feedAuthorId);
        if (followerIds.isEmpty()) {
            return;
        }

        User author = userRepository.getReferenceById(feedAuthorId);
        String title = "팔로우한 사용자의 새 피드";
        String content = "%s 님이 새 피드를 등록했어요.".formatted(author.getUsername());

        for (UUID followerId : followerIds) {
            User receiver = userRepository.getReferenceById(followerId);

            Notification notification = Notification.builder()
                .receiver(receiver)
                .title(title)
                .content(content)
                .level(NotificationLevel.INFO)
                .build();

            NotificationDto dto = saveAndMap(notification);
            notificationSseService.sendToClient(dto);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDto notifyUserFollowed(UUID followerId, UUID followeeId) {
        User follower = userRepository.getReferenceById(followerId);
        User followee = userRepository.getReferenceById(followeeId);

        Notification notification = Notification.builder()
            .receiver(followee)
            .title("새 팔로워 알림")
            .content("%s 님이 당신을 팔로우하기 시작했어요.".formatted(follower.getUsername()))
            .level(NotificationLevel.INFO)
            .build();

        NotificationDto dto = saveAndMap(notification);
        notificationSseService.sendToClient(dto);
        return dto;
    }

    /**
     * 특정 알림을 삭제합니다.
     *
     * <ul>
     *     <li>알림이 존재하지 않으면 NOT_FOUND 예외 발생</li>
     *     <li>존재하는 경우, SSE로 삭제 알림 전송 후 DB에서 제거</li>
     *     <li>삭제 알림 전송 실패 시 DB 삭제는 실행되지 않음 (재시도 로직 가능)</li>
     * </ul>
     *
     * @param notificationId 삭제할 Notification UUID
     * @throws CustomException 알림이 존재하지 않을 경우 발생
     */
    @Override
    @Transactional
    public void deleteNotification(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // SSE 전송 (실제 삭제 알림)
        NotificationDto dto = notificationMapper.toDto(notification);
        try {
            notificationSseService.sendToClient(dto); // 삭제 전송
            notificationRepository.deleteById(notificationId); // DB 삭제
            log.info("[NotificationServiceImpl] SSE 전송 후 알림 삭제 완료 : 알림ID = {}", notificationId);
        } catch (Exception e) {
            log.warn("[NotificationServiceImpl] SSE 전송 실패, 삭제 미실행 : 알림ID = {}, 메시지: {}", notificationId, e.getMessage());
        }
    }

    /**
     * 누락된 알림 조회
     *
     * <ul>
     *     <li>LastEventId 이후 생성된 알림 조회</li>
     *     <li>NotificationDto로 변환 후 반환</li>
     *     <li>유효하지 않은 LastEventId면 빈 리스트 반환</li>
     * </ul>
     *
     * @param receiverId 알림 수신자 UUID
     * @param lastEventId 마지막으로 수신한 알림 UUID 문자열
     * @return 누락된 알림 리스트
     */
    @Transactional(readOnly = true)
    @Override
    public List<NotificationDto> getMissedNotifications(UUID receiverId, String lastEventId) {
        if (lastEventId == null) return Collections.emptyList();

        UUID lastId;
        try {
            lastId = UUID.fromString(lastEventId);
        } catch (IllegalArgumentException e) {
            log.warn("[NotificationService] 유효하지 않은 LastEventId: {}", lastEventId);
            return Collections.emptyList();
        }

        log.info("[NotificationService] 누락 알림 조회 — 사용자: {}, LastEventId: {}", receiverId, lastId);

        List<Notification> missed = notificationRepository.findByReceiverIdAndIdAfter(receiverId, lastId);
        log.debug("[NotificationService] 누락 알림 조회 완료 : 수={}", missed.size());

        return missed.stream()
            .map(notificationMapper::toDto)
            .toList();
    }

    /**
     * <p>Notification 엔티티를 DB에 저장하고 DTO로 변환</p>
     * <ul>
     *     <li>DB 저장 및 flush</li>
     *     <li>DTO 변환 후 반환</li>
     *     <li>SSE 전송은 외부에서 수행</li>
     * </ul>
     *
     * @param notification 저장할 Notification 엔티티
     * @return 변환된 NotificationDto
     */
    private NotificationDto saveAndMap(Notification notification) {
        Notification saved = notificationRepository.saveAndFlush(notification);
        log.debug("[NotificationService] 알림 DB 저장 완료 : {}", saved.getId());

        return notificationMapper.toDto(saved);
    }
}
