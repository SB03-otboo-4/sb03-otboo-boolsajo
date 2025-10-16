package com.sprint.otboo.notification.repository;

import com.sprint.otboo.notification.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {
    long countByReceiverId(UUID receiverId);

    @Query("select n from Notification n where n.receiver.id = :receiverId "
        + "and (:lastId is null or n.id > :lastId)")
    List<Notification> findByReceiverIdAndIdAfter(
        @Param("receiverId") UUID receiverId,
        @Param("lastId") UUID lastId);
}
