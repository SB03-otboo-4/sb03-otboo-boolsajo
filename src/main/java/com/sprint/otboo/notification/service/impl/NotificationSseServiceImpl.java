package com.sprint.otboo.notification.service.impl;

import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.service.NotificationSseService;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseServiceImpl implements NotificationSseService {

    private Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(UUID receiverId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(receiverId, emitter);

        emitter.onCompletion(() -> emitters.remove(receiverId));
        emitter.onTimeout(() -> emitters.remove(receiverId));
        emitter.onError(e -> emitters.remove(receiverId));

        try {
            emitter.send(SseEmitter.event()
                .name("connect")
                .data("connected successfully"));
        } catch (IOException e) {
            emitters.remove(receiverId);
        }

        return emitter;
    }

    @Override
    public void sendToClient(NotificationDto dto) {
        SseEmitter emitter = emitters.get(dto.receiverId());
        if (emitter == null) {
            log.warn("[SSE] No active emitter for user: {}", dto.receiverId());
            return;
        }

        try {
            log.info("[SSE] Sending notification to {}", dto.receiverId());
            emitter.send(SseEmitter.event()
                .id(dto.id().toString())
                .name("notification")
                .data(dto));
        } catch (IOException e) {
            log.error("[SSE] Failed to send to {}", dto.receiverId(), e);
            emitters.remove(dto.receiverId());
        }
    }

    @Override
    public void removeEmitter(UUID receiverId) {
        emitters.remove(receiverId);
    }

    /** 테스트용: emitters 맵 직접 주입 */
    public void setEmitters(Map<UUID, SseEmitter> emitters) {
        this.emitters = emitters;
    }

    public Map<UUID, SseEmitter> getEmitters() {
        return emitters;
    }
}
