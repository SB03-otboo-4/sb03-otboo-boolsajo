package com.sprint.otboo.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.entity.NotificationLevel;
import com.sprint.otboo.notification.service.impl.NotificationSseServiceImpl;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    private NotificationSseServiceImpl notificationSseService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    @Test
    void SSE_구독_요청_시_초기_연결_이벤트_전송() throws Exception {
        // given: 사용자 ID와 SseEmitter 생성, 서비스 mock 설정, 인증 사용자 mock 생성
        UUID userId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        when(notificationSseService.subscribe(eq(userId), any())).thenReturn(emitter);

        CustomUserDetails customUser = mock(CustomUserDetails.class);
        when(customUser.getUserId()).thenReturn(userId);
        when(customUser.getAuthorities()).thenAnswer(invocation ->
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // when: SSE 구독 요청 수행
        mockMvc.perform(get("/api/sse")
                .with(authentication(new UsernamePasswordAuthenticationToken(customUser, null, customUser.getAuthorities())))
                .param("LastEventId", ""))

            // then: 상태 코드 200, Content-Type 확인
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void 알림_전송_시_SseEmitter를_통해_NotificationDto_전달() throws IOException {
        // given: 사용자 ID와 mock SseEmitter 생성, 실제 서비스에 emitters 등록, NotificationDto 생성
        UUID userId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);

        NotificationSseServiceImpl realService = new NotificationSseServiceImpl();
        realService.setEmitters(Map.of(userId, mockEmitter));

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
        // given: 없음, 비로그인 상태 테스트라 준비 없음

        // when: SSE 구독 요청 수행
        mockMvc.perform(get("/api/sse")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .with(anonymous()))

            // then: 상태 코드 401, 서비스 호출 없음
            .andExpect(status().isUnauthorized());

        then(notificationSseService).shouldHaveNoInteractions();
    }
}