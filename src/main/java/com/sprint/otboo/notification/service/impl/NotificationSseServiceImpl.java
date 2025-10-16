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

    /** 사용자별 다중 SSE emitter 저장 */
    private final Map<UUID, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /** 역할별 SSE emitter 저장 */
    private final Map<Role, List<SseEmitter>> roleEmitters = new ConcurrentHashMap<>();

    /**
     * 인증된 사용자의 SSE 구독 처리
     * @param receiverId 사용자 UUID
     * @param role 사용자 역할
     * @param lastEventId 마지막 수신 이벤트 ID (재연결 시 누락 알림 전송용)
     * @return 생성된 SseEmitter
     */
    @Override
    public SseEmitter subscribe(UUID receiverId, Role role, String lastEventId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        log.info("[SSE] 구독 생성 — 사용자: {}, 역할: {}, LastEventId: {}", receiverId, role, lastEventId);

        // 사용자별 다중 emitter 등록
        userEmitters.computeIfAbsent(receiverId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        roleEmitters.computeIfAbsent(role, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 종료, 타임아웃, 오류 처리
        emitter.onCompletion(() -> removeEmitter(receiverId, role, emitter));
        emitter.onTimeout(() -> removeEmitter(receiverId, role, emitter));
        emitter.onError(e -> {
            log.warn("[SSE] Emitter 오류 — 사용자: {}, 메시지: {}", receiverId, e.getMessage());
            removeEmitter(receiverId, role, emitter);
        });

        // 초기 연결 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                .name("connect")
                .data("connected successfully"));
        } catch (IOException e) {
            log.error("[SSE] 초기 connect 이벤트 전송 실패 — 사용자: {}, 메시지: {}", receiverId, e.getMessage());
            removeEmitter(receiverId, role, emitter);
        }

        return emitter;
    }

    /**
     * 특정 사용자에게 NotificationDto 이벤트 전송
     *
     * @param dto 전송할 NotificationDto
     */
    @Override
    public void sendToClient(NotificationDto dto) {
        List<SseEmitter> emitters = userEmitters.get(dto.receiverId());
        if (emitters == null || emitters.isEmpty()) {
            log.warn("[SSE] 활성 emitter 없음 — 사용자: {}", dto.receiverId());
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .id(dto.id().toString())
                    .name("notifications")
                    .data(dto));
            } catch (IOException e) {
                log.warn("[SSE] 전송 실패 — 사용자: {}, 메시지: {}", dto.receiverId(), e.getMessage());
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    /**
     * 특정 역할에 속한 모든 사용자에게 NotificationDto 이벤트 브로드캐스트
     *
     * @param role 브로드캐스트 대상 역할
     * @param dto  전송할 NotificationDto
     */
    @Override
    public void sendToRole(Role role, NotificationDto dto) {
        List<SseEmitter> emitters = roleEmitters.get(role);
        if (emitters == null || emitters.isEmpty()) return;

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .id(dto.id() != null ? dto.id().toString() : UUID.randomUUID().toString())
                    .name("notifications")
                    .data(dto));
            } catch (IOException e) {
                log.warn("[SSE] 브로드캐스트 실패 — 역할: {}, 메시지: {}", role, e.getMessage());
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    /**
     * emitter 제거
     *
     * @param receiverId 사용자 UUID
     * @param role       역할 (null 허용)
     * @param emitter    제거할 SseEmitter
     */
    public void removeEmitter(UUID receiverId, Role role, SseEmitter emitter) {
        if (receiverId != null) {
            List<SseEmitter> userList = userEmitters.get(receiverId);
            if (userList != null) userList.remove(emitter);
        }
        if (role != null) {
            List<SseEmitter> roleList = roleEmitters.get(role);
            if (roleList != null) roleList.remove(emitter);
        }
        log.debug("[SSE] Emitter 제거 — 사용자: {}, 역할: {}", receiverId, role);
    }

    /** 테스트용: emitters 맵 직접 주입 */
    public void setUserEmitters(Map<UUID, List<SseEmitter>> emitters) {
        userEmitters.clear();
        userEmitters.putAll(emitters);
    }

    public void setRoleEmitters(Map<Role, List<SseEmitter>> emitters) {
        roleEmitters.clear();
        roleEmitters.putAll(emitters);
    }

    public Map<UUID, List<SseEmitter>> getUserEmitters() {
        return userEmitters;
    }

    public Map<Role, List<SseEmitter>> getRoleEmitters() {
        return roleEmitters;
    }
}