package com.sprint.otboo.notification.repository;

import com.sprint.otboo.notification.entity.Notification;
import java.util.UUID;
import org.springframework.data.domain.Slice;

public interface NotificationRepositoryCustom {
    Slice<Notification> findByReceiverWithCursor(UUID receiverId, String cursor, UUID idAfter, int size);
}
