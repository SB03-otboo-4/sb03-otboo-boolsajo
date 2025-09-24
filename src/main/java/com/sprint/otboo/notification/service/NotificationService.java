package com.sprint.otboo.notification.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import java.util.UUID;

public interface NotificationService {

    CursorPageResponse<NotificationDto> getNotifications(UUID receiverId, NotificationQueryParams query);
}
