package com.sprint.otboo.common.util;


import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.TokenProvider;
import java.text.ParseException;
import java.util.HashMap;
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
        // 항상 mutable 로 전환
        SimpMessageHeaderAccessor simp = SimpMessageHeaderAccessor.wrap(message);
        simp.setLeaveMutable(true);

        // 세션 attributes 맵이 null이면 강제로 생성/주입
        Map<String, Object> attrs = simp.getSessionAttributes();
        if (attrs == null) {
            attrs = new HashMap<>();
            simp.setHeader(SimpMessageHeaderAccessor.SESSION_ATTRIBUTES, attrs);
        }

        StompHeaderAccessor stomp = StompHeaderAccessor.wrap(message);
        StompCommand command = stomp.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(stomp, simp, attrs);
        } else if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
            handleSendOrSubscribe(stomp, simp, attrs);
        }

        return MessageBuilder.createMessage(message.getPayload(), simp.getMessageHeaders());
    }

    private void handleConnect(StompHeaderAccessor stomp, SimpMessageHeaderAccessor simp, Map<String, Object> attrs) {
        String authHeader = stomp.getFirstNativeHeader("Authorization");
        if (authHeader == null) authHeader = stomp.getFirstNativeHeader("authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Authentication raw = tokenProvider.getAuthentication(token);
                Authentication auth = raw;

                if (raw instanceof UsernamePasswordAuthenticationToken) {
                    UsernamePasswordAuthenticationToken up = (UsernamePasswordAuthenticationToken) raw;
                    auth = new UsernamePasswordAuthenticationToken(
                        up.getPrincipal(), up.getCredentials(), up.getAuthorities()
                    );
                }

                String userIdStr = extractUserIdString(auth);
                if (userIdStr == null) {
                    log.warn("[WS] CONNECT 인증 실패: userId 파싱 불가");
                    return;
                }

                // 세션 저장 + STOMP principal은 UUID 문자열을 name으로 갖는 Principal 로 세팅
                attrs.put(ATTR_USER_ID, userIdStr);
                simp.setUser(new WsPrincipal(userIdStr));

                // SecurityContext 에는 Authentication 유지
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
            Object uidObj = attrs.get(ATTR_USER_ID);
            if (uidObj instanceof String) {
                String uidStr = (String) uidObj;
                if (!uidStr.isBlank()) {
                    simp.setUser(new WsPrincipal(uidStr));
                    log.debug("[WS] {} principal 복구(세션) → session={}, userId={}",
                        stomp.getCommand(), simp.getSessionId(), uidStr);
                    return;
                }
            }

            Authentication ctx = SecurityContextHolder.getContext().getAuthentication();
            if (ctx != null) {
                String userId = extractUserIdString(ctx);
                if (userId != null) {
                    simp.setUser(new WsPrincipal(userId));
                    attrs.put(ATTR_USER_ID, userId);
                    log.debug("[WS] {} principal 복구(SecurityContext) → session={}, userId={}",
                        stomp.getCommand(), simp.getSessionId(), userId);
                }
            }
        }
    }

    private String extractUserIdString(Authentication auth) {
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof CustomUserDetails) {
            Object u = ((CustomUserDetails) p).getUserId();
            try {
                UUID uuid = (u instanceof UUID) ? (UUID) u : UUID.fromString(String.valueOf(u));
                return uuid.toString();
            } catch (Exception ignore) {
                return null;
            }
        }
        try {
            return UUID.fromString(auth.getName()).toString();
        } catch (Exception ignore) {
            return null;
        }
    }
}
