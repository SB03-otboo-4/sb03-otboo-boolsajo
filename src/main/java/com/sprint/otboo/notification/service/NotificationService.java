package com.sprint.otboo.notification.service;

import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
import com.sprint.otboo.notification.dto.response.NotificationCursorResponse;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.user.entity.Role;
import java.util.UUID;

public interface NotificationService {

    NotificationCursorResponse getNotifications(UUID receiverId, NotificationQueryParams query);

    void deleteNotification(UUID notificationId);

    NotificationDto notifyRoleChanged(UUID receiverId, Role newRole);

    void notifyClothesAttributeCreatedForAllUsers(String attributeName);

    NotificationDto notifyFeedLiked(UUID feedAuthorId, UUID likedByUserId);

    NotificationDto notifyFeedCommented(UUID feedAuthorId, UUID commentedByUserId);

    void notifyFollowersFeedCreated(UUID feedAuthorId, UUID feedId);

    NotificationDto notifyUserFollowed(UUID followerId, UUID followeeId);
}
