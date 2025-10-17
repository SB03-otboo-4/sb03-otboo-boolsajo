package com.sprint.otboo.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.entity.NotificationLevel;
import com.sprint.otboo.notification.service.NotificationService;
import com.sprint.otboo.notification.service.NotificationSseService;
import com.sprint.otboo.notification.service.impl.NotificationSseServiceImpl;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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
    private NotificationService notificationService;

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
            null,
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
        realService.setUserEmitters(Map.of(userId, new CopyOnWriteArrayList<>(List.of(mockEmitter))));
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
        // when: 비로그인 상태로 SSE 구독 요청
        mockMvc.perform(get("/api/sse")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .with(anonymous()))
            // then: 401 Unauthorized 응답, 서비스 호출 없음
            .andExpect(status().isUnauthorized());

        then(notificationSseService).shouldHaveNoInteractions();
    }

    @Test
    void 누락_알림_전송_테스트() throws Exception {
        // given: 사용자 정보, Role, CustomUserDetails, SseEmitter, 누락 알림 준비
        UUID userId = UUID.randomUUID();
        Role role = Role.USER;
        String lastEventId = UUID.randomUUID().toString();

        UserDto userDto = new UserDto(
            userId,
            Instant.now(),
            "test@example.com",
            "Test User",
            role,
            null,
            false
        );
        CustomUserDetails customUser = new CustomUserDetails(userDto, "password");

        // 누락 알림 생성
        NotificationDto missedNotification = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            userId,
            "누락 알림 제목",
            "누락 알림 내용",
            NotificationLevel.INFO
        );

        given(notificationService.getMissedNotifications(userId, lastEventId))
            .willReturn(List.of(missedNotification));

        SseEmitter sseEmitter = mock(SseEmitter.class);
        given(notificationSseService.subscribe(userId, role, lastEventId))
            .willReturn(sseEmitter);

        // when: SSE 구독 요청
        mockMvc.perform(get("/api/sse")
                .param("LastEventId", lastEventId)
                .with(user(customUser))
                .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        // then: 누락 알림 전송 검증
        verify(notificationSseService, times(1)).sendToClient(missedNotification);
        verify(sseEmitter, never()).send(Mockito.<SseEmitter.SseEventBuilder>any());
    }

    @Test
    void lastEventId_null이면_누락_알림_전송_없이_구독() throws Exception {
        UUID userId = UUID.randomUUID();
        Role role = Role.USER;
        UserDto userDto = new UserDto(userId, Instant.now(), "test@example.com", "Test User", role, null, false);
        CustomUserDetails customUser = new CustomUserDetails(userDto, "password");

        // given: SSE 구독 시 SseEmitter 반환, lastEventId 없음
        SseEmitter sseEmitter = mock(SseEmitter.class);
        given(notificationSseService.subscribe(userId, role, null)).willReturn(sseEmitter);

        // when: SSE 구독 요청
        mockMvc.perform(get("/api/sse")
                .with(user(customUser))
                .accept(MediaType.TEXT_EVENT_STREAM))
            // then: 상태 OK, 누락 알림 전송 없음
            .andExpect(status().isOk());

        // 누락 알림 전송이 호출되지 않아야 함
        verify(notificationService, never()).getMissedNotifications(any(), any());
        verify(notificationSseService, never()).sendToClient(any());
    }

    @Test
    void subscribe_중_예외발생시_서버에러_반환() throws Exception {
        UUID userId = UUID.randomUUID();
        Role role = Role.USER;
        UserDto userDto = new UserDto(userId, Instant.now(), "test@example.com", "Test User", role, null, false);
        CustomUserDetails customUser = new CustomUserDetails(userDto, "password");

        // given: SSE 구독 실패 시 RuntimeException 발생
        given(notificationSseService.subscribe(userId, role, null))
            .willThrow(new RuntimeException("SSE 구독 실패"));

        // when: SSE 구독 요청
        mockMvc.perform(get("/api/sse")
                .with(user(customUser))
                .accept(MediaType.APPLICATION_JSON))
            // then: 서버 에러 반환
            .andExpect(status().isInternalServerError());
    }

    @Test
    void 누락_알림_여러개_순서대로_전송() throws Exception {
        UUID userId = UUID.randomUUID();
        Role role = Role.USER;
        String lastEventId = UUID.randomUUID().toString();
        UserDto userDto = new UserDto(userId, Instant.now(), "test@example.com", "Test User", role, null, false);
        CustomUserDetails customUser = new CustomUserDetails(userDto, "password");

        // given: SSE 구독 시 SseEmitter 반환, 누락 알림 2개 존재
        SseEmitter sseEmitter = mock(SseEmitter.class);
        given(notificationSseService.subscribe(userId, role, lastEventId)).willReturn(sseEmitter);

        NotificationDto n1 = new NotificationDto(UUID.randomUUID(), Instant.now(), userId, "A", "내용1", NotificationLevel.INFO);
        NotificationDto n2 = new NotificationDto(UUID.randomUUID(), Instant.now(), userId, "B", "내용2", NotificationLevel.INFO);

        given(notificationService.getMissedNotifications(userId, lastEventId)).willReturn(List.of(n1, n2));

        // when: SSE 구독 요청, LastEventId 전달
        mockMvc.perform(get("/api/sse")
                .param("LastEventId", lastEventId)
                .with(user(customUser))
                .accept(MediaType.TEXT_EVENT_STREAM))
            // then: 상태 OK, 알림 순서대로 전송
            .andExpect(status().isOk());

        InOrder inOrder = inOrder(notificationSseService);
        inOrder.verify(notificationSseService).sendToClient(n1);
        inOrder.verify(notificationSseService).sendToClient(n2);
    }
}