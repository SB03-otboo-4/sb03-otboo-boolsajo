package com.sprint.otboo.common.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("WebSocketAuthChannelInterceptor 테스트")
class WebSocketAuthChannelInterceptorTest {

    private TokenProvider tokenProvider;
    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(TokenProvider.class);
        interceptor = new WebSocketAuthChannelInterceptor(tokenProvider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static CustomUserDetails cud(UUID id) {
        UserDto dto = new UserDto(
            id, Instant.now(), "me@otboo.dev", "me", Role.USER, null, false
        );
        return CustomUserDetails.builder().userDto(dto).password("x").build();
    }

    private static Authentication authed(CustomUserDetails principal) {
        return new UsernamePasswordAuthenticationToken(principal, "N/A", principal.getAuthorities());
    }

    // ★ 세션 맵 보장 유틸
    private static void ensureSessionMap(SimpMessageHeaderAccessor simp) {
        if (simp.getSessionAttributes() == null) {
            simp.setHeader(SimpMessageHeaderAccessor.SESSION_ATTRIBUTES, new HashMap<String, Object>());
        }
    }

    private static Message<?> connectFrameWithHeader(String headerName, String headerValue, String sessionId) {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (headerName != null) {
            acc.setNativeHeader(headerName, headerValue);
        }
        acc.setSessionId(sessionId);
        acc.setLeaveMutable(true);

        SimpMessageHeaderAccessor simp = SimpMessageHeaderAccessor.wrap(
            MessageBuilder.createMessage("", acc.getMessageHeaders())
        );
        simp.setSessionId(sessionId);
        simp.setLeaveMutable(true);
        ensureSessionMap(simp); // ★ 세션 맵 보장

        return MessageBuilder.createMessage("", simp.getMessageHeaders());
    }

    private static Message<?> sendOrSubscribeFrame(StompCommand cmd, String sessionId, boolean wipeUser) {
        StompHeaderAccessor acc = StompHeaderAccessor.create(cmd);
        acc.setSessionId(sessionId);
        acc.setLeaveMutable(true);
        Message<?> msg = MessageBuilder.createMessage("", acc.getMessageHeaders());

        if (wipeUser) {
            SimpMessageHeaderAccessor simp = SimpMessageHeaderAccessor.wrap(msg);
            simp.setLeaveMutable(true);
            ensureSessionMap(simp); // ★ 세션 맵 보장
            simp.setUser(null);
            msg = MessageBuilder.createMessage("", simp.getMessageHeaders());
        }
        return msg;
    }

    @Test
    void CONNECT_성공_Authorization_대문자헤더() throws Exception {
        UUID uid = UUID.randomUUID();
        CustomUserDetails principal = cud(uid);
        Authentication auth = authed(principal);

        when(tokenProvider.getAuthentication("good")).thenReturn(auth);

        Message<?> msg = connectFrameWithHeader("Authorization", "Bearer good", "sid-1");
        Message<?> out = interceptor.preSend(msg, mock(MessageChannel.class));

        StompHeaderAccessor acc = StompHeaderAccessor.wrap(out);
        assertNotNull(acc.getUser(), "CONNECT 후 principal 이 설정되어야 함");
        assertEquals(uid.toString(), acc.getUser().getName(), "WsPrincipal 이름은 UUID 문자열이어야 함");

        SimpMessageHeaderAccessor simp = SimpMessageHeaderAccessor.wrap(out);
        Object stored = simp.getSessionAttributes().get("WS_USER_ID");
        assertEquals(uid.toString(), stored);

        Authentication ctx = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(ctx);
    }

    @Test
    void CONNECT_성공_authorization_소문자헤더도_인식() throws Exception {
        UUID uid = UUID.randomUUID();
        CustomUserDetails principal = cud(uid);
        Authentication auth = authed(principal);
        when(tokenProvider.getAuthentication("lower")).thenReturn(auth);

        Message<?> msg = connectFrameWithHeader("authorization", "Bearer lower", "sid-2");
        Message<?> out = interceptor.preSend(msg, mock(MessageChannel.class));

        StompHeaderAccessor acc = StompHeaderAccessor.wrap(out);
        assertNotNull(acc.getUser());
        assertEquals(uid.toString(), acc.getUser().getName());

        SimpMessageHeaderAccessor simp = SimpMessageHeaderAccessor.wrap(out);
        assertEquals(uid.toString(), simp.getSessionAttributes().get("WS_USER_ID"));
    }

    @Test
    void CONNECT_토큰파싱실패_예외삼켜지고_익명() throws Exception {
        when(tokenProvider.getAuthentication("bad")).thenThrow(new ParseException("bad", 0));

        Message<?> msg = connectFrameWithHeader("Authorization", "Bearer bad", "sid-bad");
        Message<?> out = interceptor.preSend(msg, mock(MessageChannel.class));

        StompHeaderAccessor acc = StompHeaderAccessor.wrap(out);
        assertNull(acc.getUser(), "파싱 실패 시 principal 미설정");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void CONNECT_헤더없음_익명_유지() {
        Message<?> msg = connectFrameWithHeader(null, null, "sid-non");
        Message<?> out = interceptor.preSend(msg, mock(MessageChannel.class));

        StompHeaderAccessor acc = StompHeaderAccessor.wrap(out);
        assertNull(acc.getUser());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void SEND_세션에_저장된_WS_USER_ID로_principal_복구() {
        // 1) SEND 프레임 생성
        Message<?> sendMsg = sendOrSubscribeFrame(StompCommand.SEND, "sid-send", true);

        // 2) 이 메시지의 세션 Attribute 맵에 직접 WS_USER_ID 주입
        SimpMessageHeaderAccessor simp = SimpMessageHeaderAccessor.wrap(sendMsg);
        simp.setLeaveMutable(true);
        ensureSessionMap(simp); // 세션 맵 보장 유틸 (이미 있으니 그대로 사용)
        simp.getSessionAttributes().put("WS_USER_ID", "11111111-1111-1111-1111-111111111111");

        // 3) 주입한 헤더로 메시지 재생성 후 검증 대상 호출
        Message<?> out = interceptor.preSend(
            MessageBuilder.createMessage(sendMsg.getPayload(), simp.getMessageHeaders()),
            mock(MessageChannel.class)
        );

        StompHeaderAccessor acc = StompHeaderAccessor.wrap(out);
        assertNotNull(acc.getUser());
        assertEquals("11111111-1111-1111-1111-111111111111", acc.getUser().getName());
    }

    @Test
    void SUBSCRIBE_SecurityContext에서_principal_UUID로_복구() {
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("22222222-2222-2222-2222-222222222222", "N/A", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Message<?> subMsg = sendOrSubscribeFrame(StompCommand.SUBSCRIBE, "sid-sub", true);
        Message<?> out = interceptor.preSend(subMsg, mock(MessageChannel.class));

        StompHeaderAccessor acc = StompHeaderAccessor.wrap(out);
        assertNotNull(acc.getUser());
        assertEquals("22222222-2222-2222-2222-222222222222", acc.getUser().getName());
    }

    @Test
    void CONNECT_헤더는있으나_Bearer_없으면_무시() {
        Message<?> msg = connectFrameWithHeader("Authorization", "Token something", "sid-x");
        Message<?> out = interceptor.preSend(msg, mock(MessageChannel.class));
        assertNull(SimpMessageHeaderAccessor.wrap(out).getUser());
    }

    @Test
    void CONNECT_userId_파싱불가하면_등록포기() throws Exception {
        Authentication raw = mock(Authentication.class);
        when(tokenProvider.getAuthentication("tok")).thenReturn(raw);
        when(raw.getPrincipal()).thenReturn(new Object());
        when(raw.getName()).thenReturn("not-uuid");

        Message<?> msg = connectFrameWithHeader("Authorization", "Bearer tok", "sid-y");
        Message<?> out = interceptor.preSend(msg, mock(MessageChannel.class));

        SimpMessageHeaderAccessor simp = SimpMessageHeaderAccessor.wrap(out);
        assertNull(simp.getUser());
        assertNull(simp.getSessionAttributes().get("WS_USER_ID"));
    }

    @Test
    void CONNECT_raw가_UsernamePasswordAuthenticationToken_이아니면_그대로사용() throws Exception {
        Authentication raw = mock(Authentication.class);
        UUID uid = UUID.randomUUID();
        when(raw.getPrincipal()).thenReturn(cud(uid));
        when(raw.getName()).thenReturn(uid.toString());
        when(tokenProvider.getAuthentication("z")).thenReturn(raw);

        Message<?> msg = connectFrameWithHeader("Authorization", "Bearer z", "sid-z");
        Message<?> out = interceptor.preSend(msg, mock(MessageChannel.class));
        assertNotNull(SimpMessageHeaderAccessor.wrap(out).getUser());
    }

    @Test
    void SEND_세션과_컨텍스트_모두없으면_익명유지() {
        Message<?> msg = sendOrSubscribeFrame(StompCommand.SEND, "sid-a", true);
        Message<?> out = interceptor.preSend(msg, mock(MessageChannel.class));
        assertNull(SimpMessageHeaderAccessor.wrap(out).getUser());
    }

    @Test
    void SUBSCRIBE_세션_userId_공백이면_컨텍스트로_복구() {
        Message<?> msg = sendOrSubscribeFrame(StompCommand.SUBSCRIBE, "sid-b", true);
        SimpMessageHeaderAccessor simp = SimpMessageHeaderAccessor.wrap(msg);
        ensureSessionMap(simp);
        simp.getSessionAttributes().put("WS_USER_ID", "   ");

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), "N/A");
        SecurityContextHolder.getContext().setAuthentication(auth);

        Message<?> out = interceptor.preSend(
            MessageBuilder.createMessage(msg.getPayload(), simp.getMessageHeaders()),
            mock(MessageChannel.class)
        );
        assertNotNull(SimpMessageHeaderAccessor.wrap(out).getUser());
    }

    @Test
    void preSend_알수없는_커맨드면_그대로반환() {
        SimpMessageHeaderAccessor simp = SimpMessageHeaderAccessor.create();
        ensureSessionMap(simp);
        Message<?> msg = MessageBuilder.createMessage("", simp.getMessageHeaders());

        Message<?> out = interceptor.preSend(msg, mock(MessageChannel.class));
        assertNotNull(out);
    }
}
