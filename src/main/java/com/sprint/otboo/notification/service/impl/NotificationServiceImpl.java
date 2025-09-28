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
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
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
    public CursorPageResponse<NotificationDto> getNotifications(UUID receiverId, NotificationQueryParams query) {
        var slice = notificationRepository.findByReceiverWithCursor(
            receiverId,
            query.parsedCursor(),
            query.idAfter(),
            query.fetchSize()
        );

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
        User receiver = userRepository.getReferenceById(receiverId);

        Notification notification = Notification.builder()
            .receiver(receiver)
            .title("권한이 변경 되었습니다.")
            .content("변경된 권한은 %s 입니다.".formatted(newRole.name()))
            .level(NotificationLevel.INFO)
            .build();

        Notification saved = notificationRepository.saveAndFlush(notification);
        return notificationMapper.toDto(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyClothesAttributeCreatedForAllUsers(String attributeName) {
        List<User> receivers = userRepository.findAll();   // 필요시 역할로 필터링
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

        notificationRepository.saveAllAndFlush(notifications);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDto notifyFeedLiked(UUID feedAuthorId, UUID likedByUserId) {
        User receiver = userRepository.getReferenceById(feedAuthorId);
        User liker = userRepository.getReferenceById(likedByUserId);

        Notification notification = Notification.builder()
            .receiver(receiver)
            .title("피드에 새로운 좋아요")
            .content("%s 님이 피드를 좋아합니다.".formatted(liker.getUsername()))
            .level(NotificationLevel.INFO)
            .build();

        Notification saved = notificationRepository.saveAndFlush(notification);
        return notificationMapper.toDto(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDto notifyFeedCommented(UUID feedAuthorId, UUID commentedByUserId) {
        User receiver = userRepository.getReferenceById(feedAuthorId);
        User commenter = userRepository.getReferenceById(commentedByUserId);

        Notification notification = Notification.builder()
            .receiver(receiver)
            .title("피드에 새로운 댓글")
            .content("%s 님이 댓글을 남겼습니다.".formatted(commenter.getUsername()))
            .level(NotificationLevel.INFO)
            .build();

        Notification saved = notificationRepository.saveAndFlush(notification);
        return notificationMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notificationRepository.deleteById(notificationId);
    }
}
