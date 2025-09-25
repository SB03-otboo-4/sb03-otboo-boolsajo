package com.sprint.otboo.notification.service.impl;

import com.sprint.otboo.common.dto.CursorPageResponse;
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

        return new CursorPageResponse<>(
            data,
            last != null ? last.createdAt().toString() : null,
            last != null ? last.id().toString() : null,
            slice.hasNext(),
            data.size(),
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
            .title("권한 변경")
            .content("변경된 권한은 %s 입니다.".formatted(newRole.name()))
            .level(NotificationLevel.INFO)
            .build();

        Notification saved = notificationRepository.saveAndFlush(notification);
        return notificationMapper.toDto(saved);
    }
}
