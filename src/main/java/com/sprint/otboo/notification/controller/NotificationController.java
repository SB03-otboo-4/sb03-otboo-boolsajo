package com.sprint.otboo.notification.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public CursorPageResponse<NotificationDto> listNotifications(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid NotificationQueryParams query
    ) {
        UUID receiverId = principal.getUserId();
        return notificationService.getNotifications(receiverId, query);
    }
}
