package com.sprint.otboo.dm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.exception.dm.DMException;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.dto.request.DirectMessageSendRequest;
import com.sprint.otboo.dm.dto.response.DirectMessageProtoResponse;
import com.sprint.otboo.dm.service.DMService;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.service.UserQueryService;
import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("DM 전송 컨트롤러 테스트")
class DMMessageControllerTest {

    DMService service = mock(DMService.class);
    SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    UserQueryService userQueryService = mock(UserQueryService.class);

    DMMessageController controller = new DMMessageController(service, template, userQueryService);

    private static CustomUserDetails cud(UUID id) {
        UserDto dto = new UserDto(
            id, Instant.now(), "me@otboo.dev", "me", Role.USER, null, false
        );
        return CustomUserDetails.builder().userDto(dto).password("x").build();
    }

    private static Principal principalWithUserId(UUID id) {
        return new Principal() {
            @Override public String getName() { return id.toString(); }
        };
    }

    private static Message<byte[]> messageWithPrincipal(Principal p) {
        SimpMessageHeaderAccessor acc = SimpMessageHeaderAccessor.create();
        acc.setLeaveMutable(true);
        acc.setUser(p);
        return new GenericMessage<>(new byte[0], acc.getMessageHeaders());
    }

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 전송성공_브로드캐스트() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "hi");
        DirectMessageDto saved = new DirectMessageDto(
            UUID.randomUUID(), me, null, null, other, null, null, "hi",
            Instant.parse("2025-10-14T05:29:40Z")
        );

        when(service.sendDm(me, other, "hi")).thenReturn(saved);

        UserSummaryResponse s = new UserSummaryResponse(me, "me", null);
        UserSummaryResponse r = new UserSummaryResponse(other, "other", null);
        when(userQueryService.getSummary(me)).thenReturn(s);
        when(userQueryService.getSummary(other)).thenReturn(r);

        Principal principal = principalWithUserId(me);
        Message<?> msg = messageWithPrincipal(principal);

        controller.send(req, msg, principal);

        ArgumentCaptor<String> destCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(destCap.capture(), payloadCap.capture());

        String a = me.toString();
        String b = other.toString();
        String left  = (a.compareTo(b) <= 0) ? a : b;
        String right = (a.compareTo(b) <= 0) ? b : a;
        assertEquals("/sub/direct-messages_" + left + "_" + right, destCap.getValue());

        assertTrue(payloadCap.getValue() instanceof DirectMessageProtoResponse);
        DirectMessageProtoResponse p = (DirectMessageProtoResponse) payloadCap.getValue();
        assertEquals(saved.id(), p.id());
        assertEquals("hi", p.content());
        assertEquals(me, p.sender().userId());
        assertEquals("me", p.sender().name());
        assertEquals(other, p.receiver().userId());
        assertEquals("other", p.receiver().name());

        verify(service).sendDm(me, other, "hi");
    }

    @Test
    void auth_null_그리고_message에도_user없으면_UNAUTHORIZED() {
        SecurityContextHolder.clearContext();

        UUID other = UUID.randomUUID();
        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "hi");

        SimpMessageHeaderAccessor acc = SimpMessageHeaderAccessor.create();
        acc.setLeaveMutable(true);
        Message<?> msg = new GenericMessage<>(new byte[0], acc.getMessageHeaders());

        assertThrows(DMException.class, () -> controller.send(req, msg, null));
        verifyNoInteractions(service, template);
    }

    @Test
    void 수신자_null이면_400() {
        UUID me = UUID.randomUUID();
        DirectMessageSendRequest req = new DirectMessageSendRequest(null, "hi");

        Principal principal = principalWithUserId(me);
        Message<?> msg = messageWithPrincipal(principal);

        assertThrows(DMException.class, () -> controller.send(req, msg, principal));
        verifyNoInteractions(service, template);
    }

    @Test
    void 내용_공백이면_400() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "    ");

        Principal principal = principalWithUserId(me);
        Message<?> msg = messageWithPrincipal(principal);

        assertThrows(DMException.class, () -> controller.send(req, msg, principal));
        verifyNoInteractions(service, template);
    }

    @Test
    void 자기자신에게_전송하면_400() {
        UUID me = UUID.randomUUID();
        DirectMessageSendRequest req = new DirectMessageSendRequest(me, "hi");

        Principal principal = principalWithUserId(me);
        Message<?> msg = messageWithPrincipal(principal);

        assertThrows(DMException.class, () -> controller.send(req, msg, principal));
        verifyNoInteractions(service, template);
    }

    @Test
    void principal이_Authentication으로_들어와도_UUID_추출정상() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "yo");
        DirectMessageDto saved = new DirectMessageDto(
            UUID.randomUUID(), me, null, null, other, null, null, "yo",
            Instant.parse("2025-10-14T05:29:40Z")
        );
        when(service.sendDm(me, other, "yo")).thenReturn(saved);

        when(userQueryService.getSummary(me)).thenReturn(new UserSummaryResponse(me, "me", null));
        when(userQueryService.getSummary(other)).thenReturn(new UserSummaryResponse(other, "other", null));

        TestingAuthenticationToken authToken = new TestingAuthenticationToken(cud(me), "N/A");
        authToken.setAuthenticated(true);

        SimpMessageHeaderAccessor acc = SimpMessageHeaderAccessor.create();
        acc.setLeaveMutable(true);
        Message<?> msg = new GenericMessage<>(new byte[0], acc.getMessageHeaders());

        controller.send(req, msg, authToken);

        verify(service).sendDm(me, other, "yo");
        verify(template).convertAndSend(startsWith("/sub/direct-messages_"), any(DirectMessageProtoResponse.class));
    }

    static class ReflectPrincipal {
        private final Object id;
        ReflectPrincipal(Object id) { this.id = id; }
        public Object getId() { return id; }
    }

    @Test
    void principal_getId_리플렉션_StringUUID_OK() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "ok");
        DirectMessageDto saved = new DirectMessageDto(
            UUID.randomUUID(), me, null, null, other, null, null, "ok",
            Instant.parse("2025-10-14T05:29:40Z")
        );
        when(service.sendDm(me, other, "ok")).thenReturn(saved);
        when(userQueryService.getSummary(me)).thenReturn(new UserSummaryResponse(me, "me", null));
        when(userQueryService.getSummary(other)).thenReturn(new UserSummaryResponse(other, "other", null));

        TestingAuthenticationToken token =
            new TestingAuthenticationToken(new ReflectPrincipal(me.toString()), "N/A");
        token.setAuthenticated(true);

        Message<?> msg = messageWithPrincipal(null);
        controller.send(req, msg, token);

        verify(service).sendDm(me, other, "ok");
        verify(template).convertAndSend(startsWith("/sub/direct-messages_"), any(DirectMessageProtoResponse.class));
    }

    static class ReflectionPrincipalUuid {
        private final UUID id;
        ReflectionPrincipalUuid(UUID id) { this.id = id; }
        public UUID getId() { return id; }
    }

    @Test
    void principal_getId_리플렉션_UUID객체_OK() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        TestingAuthenticationToken token = new TestingAuthenticationToken(new ReflectionPrincipalUuid(me), "N/A");
        token.setAuthenticated(true);

        when(userQueryService.getSummary(me)).thenReturn(new UserSummaryResponse(me, "me", null));
        when(userQueryService.getSummary(other)).thenReturn(new UserSummaryResponse(other, "other", null));
        when(service.sendDm(me, other, "ok")).thenReturn(
            new DirectMessageDto(UUID.randomUUID(), me, null, null, other, null, null, "ok",
                Instant.parse("2025-10-14T05:29:40Z"))
        );

        Message<?> msg = messageWithPrincipal(null);
        controller.send(new DirectMessageSendRequest(other, "ok"), msg, token);
        verify(service).sendDm(me, other, "ok");
    }

    @Test
    void message에_simpUser만있어도_OK() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Principal simpUser = () -> me.toString();
        Message<?> msg = messageWithPrincipal(simpUser);

        when(userQueryService.getSummary(me)).thenReturn(new UserSummaryResponse(me, "me", null));
        when(userQueryService.getSummary(other)).thenReturn(new UserSummaryResponse(other, "other", null));
        when(service.sendDm(me, other, "hi")).thenReturn(
            new DirectMessageDto(UUID.randomUUID(), me, null, null, other, null, null, "hi",
                Instant.parse("2025-10-14T05:29:40Z"))
        );

        controller.send(new DirectMessageSendRequest(other, "hi"), msg, null);
        verify(service).sendDm(me, other, "hi");
    }

    @Test
    void SecurityContext에만_있어도_OK() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UsernamePasswordAuthenticationToken a =
            new UsernamePasswordAuthenticationToken(me.toString(), "N/A");
        SecurityContextHolder.getContext().setAuthentication(a);

        when(userQueryService.getSummary(me)).thenReturn(new UserSummaryResponse(me, "me", null));
        when(userQueryService.getSummary(other)).thenReturn(new UserSummaryResponse(other, "other", null));
        when(service.sendDm(me, other, "yo")).thenReturn(
            new DirectMessageDto(UUID.randomUUID(), me, null, null, other, null, null, "yo",
                Instant.parse("2025-10-14T05:29:40Z"))
        );

        Message<?> msg = messageWithPrincipal(null);
        controller.send(new DirectMessageSendRequest(other, "yo"), msg, null);
        verify(service).sendDm(me, other, "yo");
    }

    @Test
    void toUuid_null과_잘못된문자열_처리() throws Exception {
        java.lang.reflect.Method m = DMMessageController.class.getDeclaredMethod("toUuid", Object.class);
        m.setAccessible(true);
        assertNull(m.invoke(controller, new Object[]{null}));
        assertNull(m.invoke(controller, new Object[]{"bad"}));
    }

    @Test
    void principal이_Authentication이고_name이_UUID아니면_UNAUTHORIZED() {
        SecurityContextHolder.clearContext(); // ★ 안전

        UUID other = UUID.randomUUID();
        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "hi");

        // setAuthenticated(true) 금지 → 권한 리스트 받는 생성자 사용
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("not-a-uuid", "N/A", Collections.emptyList());

        SimpMessageHeaderAccessor acc = SimpMessageHeaderAccessor.create();
        acc.setLeaveMutable(true);
        Message<?> msg = new GenericMessage<>(new byte[0], acc.getMessageHeaders());

        assertThrows(DMException.class, () -> controller.send(req, msg, auth));
        verifyNoInteractions(service, template, userQueryService);
    }

    @Test
    void principal_getId가_UUID아니면_UNAUTHORIZED() {
        UUID other = UUID.randomUUID();
        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "hi");

        class BadIdPrincipal implements Principal {
            @Override public String getName() { return "ignored"; }
            public String getId() { return "not-uuid"; }
        }
        Principal principal = new BadIdPrincipal();

        SimpMessageHeaderAccessor acc = SimpMessageHeaderAccessor.create();
        acc.setLeaveMutable(true);
        Message<?> msg = new GenericMessage<>(new byte[0], acc.getMessageHeaders());

        assertThrows(DMException.class, () -> controller.send(req, msg, principal));
        verifyNoInteractions(service, template, userQueryService);
    }

    @Test
    void principal_name이_UUID면_OK() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "hello");
        DirectMessageDto saved = new DirectMessageDto(
            UUID.randomUUID(), me, null, null, other, null, null, "hello",
            Instant.parse("2025-10-14T05:29:40Z")
        );
        when(service.sendDm(me, other, "hello")).thenReturn(saved);
        when(userQueryService.getSummary(me)).thenReturn(new UserSummaryResponse(me, "me", null));
        when(userQueryService.getSummary(other)).thenReturn(new UserSummaryResponse(other, "other", null));

        Principal principal = () -> me.toString();
        Message<?> msg = messageWithPrincipal(principal);

        controller.send(req, msg, principal);

        verify(service).sendDm(me, other, "hello");
        verify(template).convertAndSend(startsWith("/sub/direct-messages_"), any(DirectMessageProtoResponse.class));
    }

    @Test
    void principal_getId가_UUID객체면_OK() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "pong");
        DirectMessageDto saved = new DirectMessageDto(
            UUID.randomUUID(), me, null, null, other, null, null, "pong",
            Instant.parse("2025-10-14T05:29:40Z")
        );
        when(service.sendDm(me, other, "pong")).thenReturn(saved);
        when(userQueryService.getSummary(me)).thenReturn(new UserSummaryResponse(me, "me", null));
        when(userQueryService.getSummary(other)).thenReturn(new UserSummaryResponse(other, "other", null));

        class UuidPrincipal implements Principal {
            @Override public String getName() { return "ignored"; }
            public UUID getId() { return me; }
        }
        Principal principal = new UuidPrincipal();

        SimpMessageHeaderAccessor acc = SimpMessageHeaderAccessor.create();
        acc.setLeaveMutable(true);
        Message<?> msg = new GenericMessage<>(new byte[0], acc.getMessageHeaders());

        controller.send(req, msg, principal);

        verify(service).sendDm(me, other, "pong");
        verify(template).convertAndSend(startsWith("/sub/direct-messages_"), any(DirectMessageProtoResponse.class));
    }

    @Test
    void principal_getId_예외던지고_name도_UUID아님_UNAUTHORIZED() {
        SecurityContextHolder.clearContext(); // ★ 중요: 이전 테스트 상태 제거

        UUID other = UUID.randomUUID();
        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "hi");

        class ThrowingIdPrincipal implements Principal {
            @Override public String getName() { return "not-uuid"; }
            public UUID getId() { throw new RuntimeException("boom"); }
        }
        Principal p = new ThrowingIdPrincipal();

        Message<?> msg = new GenericMessage<>(new byte[0]);
        assertThrows(DMException.class, () -> controller.send(req, msg, p));
        verifyNoInteractions(service, template, userQueryService); // ★ 실제로 안 들어갔는지 보증
    }
}
