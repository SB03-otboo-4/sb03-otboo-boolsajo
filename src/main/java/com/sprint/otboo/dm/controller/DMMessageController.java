package com.sprint.otboo.dm.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.dm.DMException;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.dto.request.DirectMessageSendRequest;
import com.sprint.otboo.dm.service.DMService;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

@Controller
@RequiredArgsConstructor
public class DMMessageController {

    private final DMService dmService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/direct-messages.send")
    public void send(DirectMessageSendRequest req, Authentication auth) {
        UUID senderId = requireUserId(auth);

        if (req.receiverId() == null) throw new DMException(ErrorCode.INVALID_INPUT);
        if (Objects.equals(senderId, req.receiverId()))
            throw new DMException(ErrorCode.SELF_DM_NOT_ALLOWED);
        if (!StringUtils.hasText(req.content()))
            throw new DMException(ErrorCode.INVALID_INPUT);

        DirectMessageDto saved = dmService.sendDm(senderId, req.receiverId(), req.content());

        // 수신자 고유 채널로 전송
        String dest = "/sub/direct-messages." + req.receiverId();
        messagingTemplate.convertAndSend(dest, saved);
    }

    private UUID requireUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new DMException(ErrorCode.UNAUTHORIZED);
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            Object id = cud.getUserId();
            return (id instanceof UUID) ? (UUID) id : UUID.fromString(String.valueOf(id));
        }
        try {
            Method getId = principal.getClass().getMethod("getId");
            Object id = getId.invoke(principal);
            return (id instanceof UUID) ? (UUID) id : UUID.fromString(String.valueOf(id));
        } catch (Exception e) {
            throw new DMException(ErrorCode.UNAUTHORIZED);
        }
    }
}
