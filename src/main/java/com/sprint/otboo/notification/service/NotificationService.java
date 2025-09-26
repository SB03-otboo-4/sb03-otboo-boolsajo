package com.sprint.otboo.notification.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.user.entity.Role;
import java.util.UUID;

public interface NotificationService {

    CursorPageResponse<NotificationDto> getNotifications(UUID receiverId, NotificationQueryParams query);

    NotificationDto notifyRoleChanged(UUID receiverId, Role newRole);

    void notifyClothesAttributeCreatedForAllUsers(String attributeName);

    NotificationDto notifyFeedLiked(UUID feedAuthorId, UUID likedByUserId);

    NotificationDto notifyFeedCommented(UUID feedAuthorId, UUID commentedByUserId);
}
