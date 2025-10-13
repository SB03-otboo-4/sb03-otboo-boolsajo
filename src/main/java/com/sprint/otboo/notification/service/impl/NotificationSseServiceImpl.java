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

    /**
     * 인증된 사용자의 SSE 구독을 처리합니다.
     * <p>
     * 새로운 SseEmitter를 생성하여 emitters 맵에 저장하고,
     * 연결 이벤트를 즉시 전송합니다.
     *
     * @param receiverId 사용자 UUID
     * @param lastEventId 클라이언트가 마지막으로 받은 이벤트 ID (현재 미사용)
     * @return 생성된 SseEmitter
     */
    @Override
    public SseEmitter subscribe(UUID receiverId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(receiverId, emitter);

        // 연결 종료 시 emitters에서 제거
        emitter.onCompletion(() -> emitters.remove(receiverId));
        emitter.onTimeout(() -> emitters.remove(receiverId));
        emitter.onError(e -> emitters.remove(receiverId));

        try {
            // 초기 연결 이벤트 전송
            emitter.send(SseEmitter.event()
                .name("connect")
                .data("connected successfully"));
        } catch (IOException e) {
            // 전송 실패 시 emitters에서 제거
            emitters.remove(receiverId);
        }

        return emitter;
    }

    /**
     * 특정 사용자에게 NotificationDto 이벤트를 전송합니다.
     * <p>
     * SseEmitter가 존재하지 않으면 경고 로그를 남기고 종료합니다.
     *
     * @param dto 전송할 NotificationDto
     */
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
            // 전송 실패 시 emitters에서 제거
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
}
