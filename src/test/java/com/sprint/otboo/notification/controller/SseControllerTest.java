package com.sprint.otboo.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.entity.NotificationLevel;
import com.sprint.otboo.notification.service.NotificationSseService;
import com.sprint.otboo.notification.service.impl.NotificationSseServiceImpl;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SSE 테스트")
public class SseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationSseService notificationSseService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    @Test
    void 역할_브로드캐스트_전송_테스트() throws IOException {
        // given: Role에 대한 emitters 등록
        Role role = Role.USER;
        SseEmitter mockEmitter1 = mock(SseEmitter.class);
        SseEmitter mockEmitter2 = mock(SseEmitter.class);

        NotificationSseServiceImpl realService = new NotificationSseServiceImpl();
        realService.setRoleEmitters(Map.of(role, new CopyOnWriteArrayList<>(List.of(mockEmitter1, mockEmitter2))));

        NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            null, // 브로드캐스트
            "브로드캐스트 제목",
            "브로드캐스트 내용",
            NotificationLevel.INFO
        );

        // when: 역할 브로드캐스트
        realService.sendToRole(role, dto);

        // then: 모든 emitters에 send 호출
        verify(mockEmitter1, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(mockEmitter2, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void 알림_전송_시_SseEmitter를_통해_NotificationDto_전달() throws IOException {
        // given: 사용자 ID, Role, mock SseEmitter 생성, 실제 서비스에 emitters 등록
        UUID userId = UUID.randomUUID();
        Role role = Role.USER;
        SseEmitter mockEmitter = mock(SseEmitter.class);

        NotificationSseServiceImpl realService = new NotificationSseServiceImpl();
        realService.setEmitters(Map.of(userId, mockEmitter));
        realService.setRoleEmitters(Map.of(role, new CopyOnWriteArrayList<>(List.of(mockEmitter))));

        NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            userId,
            "알림 제목",
            "알림 내용",
            NotificationLevel.INFO
        );

        // when: NotificationDto 전송
        realService.sendToClient(dto);

        // then: mockEmitter.send() 호출 여부 검증
        verify(mockEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void 비로그인_사용자는_SSE_구독_거부() throws Exception {
        // given: 없음, 비로그인 상태 테스트

        // when: SSE 구독 요청 수행
        mockMvc.perform(get("/api/sse")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .with(anonymous()))
            // then: 상태 코드 401, 서비스 호출 없음
            .andExpect(status().isUnauthorized());

        then(notificationSseService).shouldHaveNoInteractions();
    }
}