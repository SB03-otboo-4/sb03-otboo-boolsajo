package com.sprint.otboo.notification.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.notification.service.NotificationSseService;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
public class NotificationSseController {

    private final NotificationSseService notificationSseService;

    /**
     * 인증 사용자의 SSE 구독을 처리
     *
     * @param principal 인증 사용자 정보
     * @param lastEventId 마지막 수신 이벤트 ID
     * @return SseEmitter
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
        @AuthenticationPrincipal CustomUserDetails principal,
        @RequestParam(value = "LastEventId", required = false) String lastEventId
    ) {
        UUID userId = principal.getUserId();
        SseEmitter emitter = notificationSseService.subscribe(userId, lastEventId);

        // 초기 연결 이벤트 발송
        try {
            emitter.send(SseEmitter.event()
                .name("connect")
                .data("connected successfully"));
        } catch (IOException e) {
            notificationSseService.removeEmitter(userId);
        }

        return emitter;
    }
}
