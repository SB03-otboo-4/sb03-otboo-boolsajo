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

    /**
     * 인증된 사용자에 대해 커서 기반 페이지네이션으로 알림 목록을 조회
     *
     * @param principal Spring Security가 주입한 인증 정보
     * @param query cursor.idAfter.limit 등의 페이지네이션 파라미터
     * @return 알림 데이터와 페이지네이션 메타정보
     * */
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

    /**
     * 지정된 알림을 읽음처리 ( 삭제 )
     *
     * @param notificationId 삭제할 알림 식별자
     * @return 처리 완료 시 204 No Content
     * */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID notificationId) {
        log.info("[NotificationController] 알림 삭제 요청 : 알림ID = {}", notificationId);
        notificationService.deleteNotification(notificationId);
        log.info("[NotificationController] 알림 삭제 완료 : 알림ID = {}", notificationId);
        return ResponseEntity.noContent().build();
    }
}
