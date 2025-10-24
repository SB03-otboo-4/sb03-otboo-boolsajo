package com.sprint.otboo.notification.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.notification.controller.api.NotificationSseApi;
import com.sprint.otboo.notification.service.NotificationService;
import com.sprint.otboo.notification.service.NotificationSseService;
import com.sprint.otboo.user.entity.Role;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
public class NotificationSseController implements NotificationSseApi {

    private final NotificationSseService notificationSseService;
    private final NotificationService notificationService;

    /**
     * 인증 사용자의 SSE 구독을 처리합니다.
     * <ul>
     *     <li>인증된 사용자 정보를 기반으로 SSE 연결을 생성</li>
     *     <li>사용자 및 역할별 emitter 등록</li>
     * </ul>
     *
     * @param principal 인증 사용자 정보
     * @return 생성된 SseEmitter
     */
    @Override
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
        @AuthenticationPrincipal CustomUserDetails principal,
        @RequestParam(value = "LastEventId", required = false) String lastEventId
    ) {
        UUID userId = principal.getUserId();
        Role role = principal.getUserDto().role();

        log.info("[SSE] 구독 요청 — 사용자: {}, 역할: {}, LastEventId: {}", userId, role, lastEventId);

        // SSE 연결 생성
        SseEmitter emitter = notificationSseService.subscribe(userId, role, lastEventId);

        // 재연결 시 누락 알림 전송
        if (lastEventId != null) {
            notificationService.getMissedNotifications(userId, lastEventId)
                .forEach(notificationSseService::sendToClient);
        }

        return emitter;
    }
}
