package com.sprint.otboo.dm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.authentication.TestingAuthenticationToken;

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

        // UserSummaryResponse는 final일 수 있으므로 mock으로 name/profile만 스텁
        UserSummaryResponse s = mock(UserSummaryResponse.class);
        when(s.name()).thenReturn("me");
        when(s.profileImageUrl()).thenReturn(null);
        UserSummaryResponse r = mock(UserSummaryResponse.class);
        when(r.name()).thenReturn("other");
        when(r.profileImageUrl()).thenReturn(null);

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
        UUID other = UUID.randomUUID();
        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "hi");

        // user 미설정 메시지
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

        UserSummaryResponse s = mock(UserSummaryResponse.class);
        when(s.name()).thenReturn("me");
        when(s.profileImageUrl()).thenReturn(null);
        UserSummaryResponse r = mock(UserSummaryResponse.class);
        when(r.name()).thenReturn("other");
        when(r.profileImageUrl()).thenReturn(null);
        when(userQueryService.getSummary(me)).thenReturn(s);
        when(userQueryService.getSummary(other)).thenReturn(r);

        TestingAuthenticationToken authToken =
            new TestingAuthenticationToken(cud(me), "N/A");
        authToken.setAuthenticated(true);

        Principal principal = new Principal() {
            @Override public String getName() { return authToken.getName(); }
        };

        // 메시지에는 principal을 심지 않아도, 컨트롤러가 principal(Authentication) 경로에서 추출 가능
        SimpMessageHeaderAccessor acc = SimpMessageHeaderAccessor.create();
        acc.setLeaveMutable(true);
        Message<?> msg = new GenericMessage<>(new byte[0], acc.getMessageHeaders());

        controller.send(req, msg, authToken);

        verify(service).sendDm(me, other, "yo");
        verify(template).convertAndSend(startsWith("/sub/direct-messages_"), any());
    }
}
