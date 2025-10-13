package com.sprint.otboo.notification.service.impl;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
import com.sprint.otboo.notification.dto.response.NotificationCursorResponse;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.entity.Notification;
import com.sprint.otboo.notification.entity.NotificationLevel;
import com.sprint.otboo.notification.mapper.NotificationMapper;
import com.sprint.otboo.notification.repository.NotificationRepository;
import com.sprint.otboo.notification.service.NotificationService;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

        Notification saved = notificationRepository.saveAndFlush(notification);

        log.debug("[NotificationServiceImpl] 권한 변경 알림 저장 완료 : 알림ID = {}",saved.getId());
        return notificationMapper.toDto(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyClothesAttributeCreatedForAllUsers(String attributeName) {
        log.info("[NotificationServiceImpl] 의류 속성 생성 알림 브로드캐스트 시작 : 속성명 = {}", attributeName);

        List<User> receivers = userRepository.findAll();   // 필요시 역할로 필터링

        log.debug("브로드캐스트 대상 수 : {}",receivers.size());

        List<Notification> notifications = new ArrayList<>();
        for (User receiver : receivers) {
            notifications.add(
                Notification.builder()
                    .receiver(receiver)
                    .title("새 의상 속성이 등록되었습니다.")
                    .content("내 의상에 [%s] 속성을 추가해보세요.".formatted(attributeName))
                    .level(NotificationLevel.INFO)
                    .build()
            );
        }

        log.debug("[NotificationServiceImpl] 브로드캐스트 알림 준비 완료 : 총개수 = {}",notifications.size());
        notificationRepository.saveAllAndFlush(notifications);
        log.info("[NotificationServiceImpl] 브로드캐스트 알림 저장 완료 : 속성명 = {}",attributeName);
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

        Notification saved = notificationRepository.saveAndFlush(notification);

        log.debug("[NotificationServiceImpl] 피드 좋아요 알림 저장 완료 : 알림ID = {}",saved.getId());
        return notificationMapper.toDto(saved);
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

        Notification saved = notificationRepository.saveAndFlush(notification);

        log.debug("[NotificationServiceImpl] 피드 댓글 알림 저장 완료 : 알림ID = {}",saved.getId());
        return notificationMapper.toDto(saved);
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
}
