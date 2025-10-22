package com.sprint.otboo.dm.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.dm.DMException;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.dto.request.DirectMessageSendRequest;
import com.sprint.otboo.dm.dto.response.DirectMessageProtoResponse;
import com.sprint.otboo.dm.service.DMService;
import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import com.sprint.otboo.user.service.UserQueryService;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@Controller
@Slf4j
public class DMMessageController {

    private final DMService dmService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserQueryService userQueryService;

    @MessageMapping("/direct-messages_send")
    public void send(DirectMessageSendRequest request, Message<?> message, Principal principal) {
        UUID senderId = requireUserId(principal, message);

        if (request.receiverId() == null) throw new DMException(ErrorCode.INVALID_INPUT);
        if (Objects.equals(senderId, request.receiverId())) throw new DMException(ErrorCode.SELF_DM_NOT_ALLOWED);
        if (!StringUtils.hasText(request.content())) throw new DMException(ErrorCode.INVALID_INPUT);

        DirectMessageDto saved = dmService.sendDm(senderId, request.receiverId(), request.content());

        // 프로토타입 스키마로 변환
        UserSummaryResponse s = userQueryService.getSummary(senderId);
        UserSummaryResponse r = userQueryService.getSummary(request.receiverId());
        DirectMessageProtoResponse payload = DirectMessageProtoResponse.from(saved, s, r);

        // /sub/direct-messages_<작은UUID>_<큰UUID>
        String a = senderId.toString();
        String b = request.receiverId().toString();
        String left  = (a.compareTo(b) <= 0) ? a : b;
        String right = (a.compareTo(b) <= 0) ? b : a;

        messagingTemplate.convertAndSend("/sub/direct-messages_" + left + "_" + right, payload);
    }

    private UUID requireUserId(Principal principal, Message<?> message) {
        if (principal != null) {
            UUID uid = extractFromPrincipal(principal);
            if (uid != null) return uid;
        }
        Principal simpUser = SimpMessageHeaderAccessor.wrap(message).getUser();
        if (simpUser != null) {
            UUID uid = extractFromPrincipal(simpUser);
            if (uid != null) return uid;
        }
        Authentication ctx = SecurityContextHolder.getContext().getAuthentication();
        if (ctx != null) {
            UUID uid = extractFromAuthentication(ctx);
            if (uid != null) return uid;
        }
        throw new DMException(ErrorCode.UNAUTHORIZED);
    }

    private UUID extractFromPrincipal(Principal p) {
        if (p instanceof Authentication auth) {
            UUID uid = extractFromAuthentication(auth);
            if (uid != null) return uid;
        }
        if (p instanceof CustomUserDetails cud) {
            Object id = cud.getUserId();
            return toUuid(id);
        }
        try {
            Method m = p.getClass().getMethod("getId");
            Object id = m.invoke(p);
            return toUuid(id);
        } catch (Exception ignore) {}
        try { return UUID.fromString(p.getName()); } catch (Exception ignore) {}
        return null;
    }

    private UUID extractFromAuthentication(Authentication auth) {
        Object inner = auth.getPrincipal();
        if (inner instanceof CustomUserDetails cud) {
            return toUuid(cud.getUserId());
        }
        try { return UUID.fromString(auth.getName()); } catch (Exception ignore) {}
        return null;
    }

    private UUID toUuid(Object v) {
        try { return (v instanceof UUID) ? (UUID) v : UUID.fromString(String.valueOf(v)); }
        catch (Exception e) { return null; }
    }
}
