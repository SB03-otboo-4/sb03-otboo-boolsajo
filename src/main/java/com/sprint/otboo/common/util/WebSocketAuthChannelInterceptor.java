package com.sprint.otboo.common.util;


import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.TokenProvider;
import java.text.ParseException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final TokenProvider tokenProvider;
    private static final String ATTR_USER_ID = "WS_USER_ID";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor simpAccessor = SimpMessageHeaderAccessor.wrap(message);
        simpAccessor.setLeaveMutable(true);

        StompHeaderAccessor stompAccessor = StompHeaderAccessor.wrap(message);
        StompCommand command = stompAccessor.getCommand();

        Map<String, Object> attrs = simpAccessor.getSessionAttributes(); // non-null 보장

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(stompAccessor, simpAccessor, attrs);
        } else if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
            handleSendOrSubscribe(stompAccessor, simpAccessor, attrs);
        }

        return MessageBuilder.createMessage(message.getPayload(), simpAccessor.getMessageHeaders());
    }

    private void handleConnect(StompHeaderAccessor stomp, SimpMessageHeaderAccessor simp, Map<String, Object> attrs) {
        String authHeader = stomp.getFirstNativeHeader("Authorization");
        if (authHeader == null) authHeader = stomp.getFirstNativeHeader("authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Authentication raw = tokenProvider.getAuthentication(token);
                Authentication auth = raw;

                if (raw instanceof UsernamePasswordAuthenticationToken up) {
                    auth = new UsernamePasswordAuthenticationToken(
                        up.getPrincipal(), up.getCredentials(), up.getAuthorities()
                    );
                }

                String userIdStr = extractUserIdString(auth);

                attrs.put(ATTR_USER_ID, userIdStr);
                simp.setUser(auth); // principal 설정
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("[WS] CONNECT 성공 → session={}, userId={}", simp.getSessionId(), userIdStr);
            } catch (ParseException e) {
                log.warn("[WS] CONNECT 토큰 파싱 실패: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("[WS] CONNECT 인증 실패: {}", e.getMessage());
            }
        } else {
            log.debug("[WS] CONNECT Authorization 헤더 없음(익명)");
        }
    }

    private void handleSendOrSubscribe(StompHeaderAccessor stomp, SimpMessageHeaderAccessor simp, Map<String, Object> attrs) {
        if (simp.getUser() == null) {
            Object uid = attrs.get(ATTR_USER_ID);
            if (uid instanceof String uidStr && !uidStr.isBlank()) {
                simp.setUser(new UsernamePasswordAuthenticationToken(uidStr, null, null));
                log.debug("[WS] {} principal 복구(세션) → session={}, userId={}",
                    stomp.getCommand(), simp.getSessionId(), uidStr);
            } else {
                Authentication ctx = SecurityContextHolder.getContext().getAuthentication();
                if (ctx != null) {
                    simp.setUser(ctx);
                    String userId = extractUserIdString(ctx);
                    if (userId != null) attrs.put(ATTR_USER_ID, userId);
                    log.debug("[WS] {} principal 복구(SecurityContext) → session={}, userId={}",
                        stomp.getCommand(), simp.getSessionId(), userId);
                }
            }
        }
    }

    private String extractUserIdString(Authentication auth) {
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof CustomUserDetails cud) {
            Object u = cud.getUserId();
            UUID uuid = (u instanceof UUID) ? (UUID) u : UUID.fromString(String.valueOf(u));
            return uuid.toString();
        }
        try {
            return UUID.fromString(auth.getName()).toString();
        } catch (Exception ignore) {
            return null;
        }
    }
}
