package com.sprint.otboo.dm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.exception.dm.DMException;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.dto.request.DirectMessageSendRequest;
import com.sprint.otboo.dm.service.DMService;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@DisplayName("DM 전송 컨트롤러 테스트")
class DMMessageControllerTest {

    DMService service = mock(DMService.class);
    SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    DMMessageController controller = new DMMessageController(service, template);

    private static CustomUserDetails cud(UUID id) {
        UserDto dto = new UserDto(
            id, Instant.now(), "me@otboo.dev", "me", Role.USER, null, false
        );
        return CustomUserDetails.builder().userDto(dto).password("x").build();
    }

    static class ReflectPrincipalUuid {
        private final UUID id;
        ReflectPrincipalUuid(UUID id) { this.id = id; }
        public UUID getId() { return id; }
    }

    static class ReflectPrincipalString {
        private final String id;
        ReflectPrincipalString(String id) { this.id = id; }
        public String getId() { return id; }
    }

    static class NoGetIdPrincipal {
        String name = "nope";
    }

    private static Authentication auth(Object principal, boolean authenticated) {
        TestingAuthenticationToken a = new TestingAuthenticationToken(principal, "N/A");
        a.setAuthenticated(authenticated);
        return a;
    }

    @Test
    void 전송성공_브로드캐스트() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "hi");
        DirectMessageDto saved = new DirectMessageDto(
            UUID.randomUUID(), me, "me", null, other, "other", null, "hi",
            Instant.parse("2025-10-14T05:29:40Z")
        );

        when(service.sendDm(me, other, "hi")).thenReturn(saved);

        Authentication auth = auth(cud(me), true);
        controller.send(req, auth);

        ArgumentCaptor<String> destCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(destCap.capture(), payloadCap.capture());

        String dest = destCap.getValue();
        DirectMessageDto payload = (DirectMessageDto) payloadCap.getValue();

        assertEquals("/sub/direct-messages." + other, dest);
        assertEquals(saved, payload);
        verify(service).sendDm(me, other, "hi");
    }

    @Test
    void auth_null_UNAUTHORIZED() {
        DirectMessageSendRequest req = new DirectMessageSendRequest(UUID.randomUUID(), "hi");
        assertThrows(DMException.class, () -> controller.send(req, null));
        verifyNoInteractions(service, template);
    }

    @Test
    void 인증되지않은토큰_UNAUTHORIZED() {
        UUID me = UUID.randomUUID();
        DirectMessageSendRequest req = new DirectMessageSendRequest(UUID.randomUUID(), "hi");
        Authentication unauth = auth(cud(me), false);

        assertThrows(DMException.class, () -> controller.send(req, unauth));
        verifyNoInteractions(service, template);
    }

    @Test
    void 수신자_null이면_400() {
        UUID me = UUID.randomUUID();
        Authentication a = auth(cud(me), true);
        DirectMessageSendRequest req = new DirectMessageSendRequest(null, "hi");

        assertThrows(DMException.class, () -> controller.send(req, a));
        verifyNoInteractions(service, template);
    }

    @Test
    void 내용_공백이면_400() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Authentication a = auth(cud(me), true);

        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "   ");
        assertThrows(DMException.class, () -> controller.send(req, a));
        verifyNoInteractions(service, template);
    }

    @Test
    void 자기자신에게_전송하면_400() {
        UUID me = UUID.randomUUID();
        Authentication a = auth(cud(me), true);

        DirectMessageSendRequest req = new DirectMessageSendRequest(me, "hi");
        assertThrows(DMException.class, () -> controller.send(req, a));
        verifyNoInteractions(service, template);
    }

    @Test
    void 리플렉션_principal_UUID_정상동작() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "hello");
        DirectMessageDto saved = new DirectMessageDto(
            UUID.randomUUID(), me, "me", null, other, "other", null, "hello",
            Instant.parse("2025-10-14T05:29:40Z")
        );
        when(service.sendDm(me, other, "hello")).thenReturn(saved);

        Authentication a = auth(new ReflectPrincipalUuid(me), true);
        controller.send(req, a);

        verify(service).sendDm(me, other, "hello");
        verify(template).convertAndSend(eq("/sub/direct-messages." + other), eq(saved));
    }

    @Test
    void 리플렉션_principal_StringUUID_정상동작() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "yo");
        DirectMessageDto saved = new DirectMessageDto(
            UUID.randomUUID(), me, "me", null, other, "other", null, "yo",
            Instant.parse("2025-10-14T05:29:40Z")
        );
        when(service.sendDm(me, other, "yo")).thenReturn(saved);

        Authentication a = auth(new ReflectPrincipalString(me.toString()), true);
        controller.send(req, a);

        verify(service).sendDm(me, other, "yo");
        verify(template).convertAndSend(eq("/sub/direct-messages." + other), eq(saved));
    }

    @Test
    void getId없는_principal_UNAUTHORIZED() {
        UUID other = UUID.randomUUID();
        Authentication a = auth(new NoGetIdPrincipal(), true);

        DirectMessageSendRequest req = new DirectMessageSendRequest(other, "hello");
        assertThrows(DMException.class, () -> controller.send(req, a));

        verifyNoInteractions(service, template);
    }
}
