package com.sprint.otboo.notification.repository;

import com.sprint.otboo.notification.entity.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {
    long countByReceiverId(UUID receiverId);
}
