package com.sprint.otboo.notification.service.impl;

import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.service.NotificationSseService;
import com.sprint.otboo.user.entity.Role;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseServiceImpl implements NotificationSseService {

    /** 사용자별 연결된 SSE emitter */
    private final Map<UUID, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    /** 역할별 연결된 SSE emitter 목록 */
    private final Map<Role, List<SseEmitter>> roleEmitters = new ConcurrentHashMap<>();

    /**
     * 인증된 사용자의 SSE 구독을 처리합니다.
     * <ul>
     *     <li>유저별, 역할별 emitter 등록</li>
     *     <li>연결 종료·타임아웃 시 자동 제거</li>
     *     <li>초기 연결 이벤트 즉시 전송</li>
     * </ul>
     *
     * @param receiverId 사용자 UUID
     * @param role 사용자 역할
     * @param lastEventId 마지막 수신 이벤트 ID (현재 미사용)
     * @return 생성된 SseEmitter
     */
    @Override
    public SseEmitter subscribe(UUID receiverId, Role role, String lastEventId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // 사용자 emitter 등록
        userEmitters.put(receiverId, emitter);

        // 역할 emitter 목록에 추가
        roleEmitters.computeIfAbsent(role, r -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 해제 시 정리
        emitter.onCompletion(() -> removeEmitter(receiverId, role));
        emitter.onTimeout(() -> removeEmitter(receiverId, role));
        emitter.onError(e -> removeEmitter(receiverId, role));

        // 초기 연결 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                .name("connect")
                .data("connected successfully"));
        } catch (IOException e) {
            removeEmitter(receiverId, role);
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
        SseEmitter emitter = userEmitters.get(dto.receiverId());
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
            removeEmitter(dto.receiverId(), null);
        }
    }

    @Override
    public void sendToRole(Role role, NotificationDto dto) {
        List<SseEmitter> emitters = roleEmitters.get(role);
        if (emitters == null || emitters.isEmpty()) {
            log.warn("[SSE] No active emitters for role: {}", role);
            return;
        }

        log.info("[SSE] Broadcasting to role: {}", role);
        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .id(dto.id().toString())
                    .name("notification")
                    .data(dto));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
    }

    @Override
    public void removeEmitter(UUID receiverId) {
        removeEmitter(receiverId, null);
    }

    private void removeEmitter(UUID receiverId, Role role) {
        SseEmitter removed = userEmitters.remove(receiverId);

        if (role != null && removed != null) {
            List<SseEmitter> list = roleEmitters.get(role);
            if (list != null) list.remove(removed);
        }
    }

    /** 테스트용: emitters 맵 직접 주입 */
    public void setEmitters(Map<UUID, SseEmitter> emitters) {
        userEmitters.clear();
        userEmitters.putAll(emitters);
    }

    public void setRoleEmitters(Map<Role, List<SseEmitter>> emitters) {
        roleEmitters.clear();
        roleEmitters.putAll(emitters);
    }
}
