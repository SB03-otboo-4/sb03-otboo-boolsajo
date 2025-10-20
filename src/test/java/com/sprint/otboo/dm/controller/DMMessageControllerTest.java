package com.sprint.otboo.dm.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.dto.request.DirectMessageSendRequest;
import com.sprint.otboo.dm.service.DMService;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;

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

        TestingAuthenticationToken auth =
            new TestingAuthenticationToken(cud(me), "N/A");
        auth.setAuthenticated(true);

        controller.send(req, auth);

        ArgumentCaptor<String> destCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(destCap.capture(), payloadCap.capture());

        String dest = destCap.getValue();
        DirectMessageDto payload = (DirectMessageDto) payloadCap.getValue();

        assert(dest.equals("/sub/direct-messages." + other));
        assert(payload.equals(saved));
        verify(service).sendDm(me, other, "hi");
    }
}
