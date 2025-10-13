package com.sprint.otboo.notification.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.notification.controller.api.NotificationApi;
import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
import com.sprint.otboo.notification.dto.response.NotificationCursorResponse;
import com.sprint.otboo.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationCursorResponse listNotifications(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid NotificationQueryParams query
    ) {
        UUID receiverId = principal.getUserId();
        log.debug("[NotificationController] 알림 목록 조회 시작 : 사용자 = {}, query = {}", receiverId, query);
        NotificationCursorResponse response = notificationService.getNotifications(receiverId, query);
        log.debug("[NotificationController] 알림 목록 조회 완료 : 개수 = {}, 다음 페이지 여부 = {}",
            response.data().size(), response.hasNext());
        return response;
    }


    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID notificationId) {
        log.info("[NotificationController] 알림 삭제 요청 : 알림ID = {}", notificationId);
        notificationService.deleteNotification(notificationId);
        log.info("[NotificationController] 알림 삭제 완료 : 알림ID = {}", notificationId);
        return ResponseEntity.noContent().build();
    }
}
