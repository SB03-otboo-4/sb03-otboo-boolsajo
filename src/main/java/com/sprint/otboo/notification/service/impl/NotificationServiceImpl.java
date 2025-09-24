package com.sprint.otboo.notification.service.impl;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.mapper.NotificationMapper;
import com.sprint.otboo.notification.repository.NotificationRepository;
import com.sprint.otboo.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
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

}
