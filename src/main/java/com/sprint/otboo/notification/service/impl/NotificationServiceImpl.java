package com.sprint.otboo.notification.service.impl;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
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
    private final NotificationSseService notificationSseService;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<NotificationDto> getNotifications(UUID receiverId, NotificationQueryParams query) {
        log.debug("[NotificationServiceImpl] 알림 조회 시작 : 사용자 = {}, cursor = {}, idAfter = {}, fetchSize = {}",
            receiverId, query.cursor(), query.idAfter(), query.fetchSize());

        var slice = notificationRepository.findByReceiverWithCursor(
            receiverId,
            query.parsedCursor(),
            query.idAfter(),
            query.fetchSize()
        );

        log.debug("[NotificationServiceImpl] 알림 조회 완료 : 조회수 = {}, 다음 페이지 여부 = {}",
            slice.getNumberOfElements(), slice.hasNext());

        List<NotificationDto> data = slice.getContent()
            .stream()
            .map(notificationMapper::toDto)
            .toList();

        NotificationDto last = slice.hasNext() && !data.isEmpty()
            ? data.get(data.size() - 1)
            : null;

        long totalCount = notificationRepository.countByReceiverId(receiverId);

        return new CursorPageResponse<>(
            data,
            last != null ? last.createdAt().toString() : null,
            last != null ? last.id().toString() : null,
            slice.hasNext(),
            totalCount,
            "createdAt",
            "DESCENDING"
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDto notifyRoleChanged(UUID receiverId, Role newRole) {
        log.info("[NotificationServiceImpl] 권한 변경 알림 생성 : 사용자 = {}, 새로운 권한 = {}", receiverId, newRole);

        User receiver = userRepository.getReferenceById(receiverId);

        Notification notification = Notification.builder()
            .receiver(receiver)
            .title("권한이 변경 되었습니다.")
            .content("변경된 권한은 %s 입니다.".formatted(newRole.name()))
            .level(NotificationLevel.INFO)
            .build();

        NotificationDto dto = saveAndMap(notification);

        // 권한 변경 알림은 개별 사용자에게만 전송
        notificationSseService.sendToClient(dto);
        return dto;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyClothesAttributeCreatedForAllUsers(String attributeName) {
        // ADMIN, USER 모두에게 브로드캐스트
        NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            null, // 브로드캐스트이므로 receiverId 없음
            "새 의상 속성이 등록되었습니다.",
            "내 의상에 [%s] 속성을 추가해보세요.".formatted(attributeName),
            NotificationLevel.INFO
        );

        List<User> receivers = userRepository.findAll();   // 필요시 역할로 필터링
        for (User receiver : receivers) {
            Notification notification = Notification.builder()
                .receiver(receiver)
                .title("새 의상 속성이 등록되었습니다.")
                .content("내 의상에 [%s] 속성을 추가해보세요.".formatted(attributeName))
                .level(NotificationLevel.INFO)
                .build();

            saveAndMap(notification);
        }

        notificationSseService.sendToRole(Role.USER, dto);
        notificationSseService.sendToRole(Role.ADMIN, dto);
    }

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

        // 개인 대상 전송
        notificationSseService.sendToClient(dto);
        return dto;
    }

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

        // 개인 대상 전송
        notificationSseService.sendToClient(dto);
        return dto;
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId) {
        log.info("[NotificationServiceImpl] 알림 삭제 요청 : 알림ID = {}", notificationId);

        if (!notificationRepository.existsById(notificationId)) {
            log.warn("[NotificationServiceImpl] 존재하지 않는 알림 삭제 시도 : 알림 ID = {}", notificationId);
            throw new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notificationRepository.deleteById(notificationId);
        log.info("[NotificationServiceImpl] 알림 삭제 완료 : 알림ID = {}", notificationId);
    }

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
        return notificationMapper.toDto(saved);
    }
}
