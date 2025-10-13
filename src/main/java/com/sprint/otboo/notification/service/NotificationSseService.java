package com.sprint.otboo.notification.service;

import com.sprint.otboo.notification.dto.response.NotificationDto;
import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationSseService {

    SseEmitter subscribe(UUID receiverId, String lastEventId);

    void sendToClient(NotificationDto dto);

    void removeEmitter(UUID receiverId);
}
