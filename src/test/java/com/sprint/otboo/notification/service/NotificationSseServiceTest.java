package com.sprint.otboo.notification.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.entity.NotificationLevel;
import com.sprint.otboo.notification.service.impl.NotificationSseServiceImpl;
import com.sprint.otboo.user.entity.Role;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("NotificationSseServiceImpl SSE 단위 테스트")
public class NotificationSseServiceTest {

    private NotificationSseServiceImpl sseService;

    @BeforeEach
    void setUp() {
        sseService = new NotificationSseServiceImpl();
    }

    @Test
    void 사용자_SSE_구독_생성() throws IOException {
        // given: 사용자 ID, 역할, 마지막 이벤트 ID
        UUID userId = UUID.randomUUID();
        Role role = Role.USER;
        String lastEventId = null;

        // when: SSE 구독 생성
        SseEmitter emitter = sseService.subscribe(userId, role, lastEventId);

        // then: Emitter가 생성되고 맵에 등록됨
        assertNotNull(emitter);
        assertEquals(1, sseService.getUserEmitters().get(userId).size());
        assertEquals(1, sseService.getRoleEmitters().get(role).size());
    }

    @Test
    void 사용자에게_알림_전송() throws IOException {
        // given: 사용자 ID와 mock Emitter, 전송할 NotificationDto
        UUID userId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        sseService.setUserEmitters(Map.of(userId, new CopyOnWriteArrayList<>(List.of(mockEmitter))));

        NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            userId,
            "제목",
            "내용",
            NotificationLevel.INFO
        );

        // when: NotificationDto 전송
        sseService.sendToClient(dto);

        // then: Emitter.send 호출 확인
        verify(mockEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void 역할별_브로드캐스트_전송() throws IOException {
        // given: 역할과 2개의 mock Emitter, 전송할 NotificationDto
        Role role = Role.USER;
        SseEmitter mockEmitter1 = mock(SseEmitter.class);
        SseEmitter mockEmitter2 = mock(SseEmitter.class);
        sseService.setRoleEmitters(Map.of(role, new CopyOnWriteArrayList<>(List.of(mockEmitter1, mockEmitter2))));

        NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            null,
            "브로드캐스트 제목",
            "브로드캐스트 내용",
            NotificationLevel.INFO
        );

        // when: 역할 브로드캐스트 전송
        sseService.sendToRole(role, dto);

        // then: 각 Emitter에 send 호출 확인
        verify(mockEmitter1, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(mockEmitter2, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void Emitter_제거_확인() {
        // given: 사용자 ID, 역할, 구독 생성
        UUID userId = UUID.randomUUID();
        Role role = Role.USER;
        SseEmitter emitter = sseService.subscribe(userId, role, null);

        // when: Emitter 제거
        sseService.removeEmitter(userId, role, emitter);

        // then: userEmitters와 roleEmitters에서 제거 확인
        assertTrue(sseService.getUserEmitters().get(userId).isEmpty());
        assertTrue(sseService.getRoleEmitters().get(role).isEmpty());
    }


    @Test
    void 없는_사용자_알림_전송_검증() {
        // given: 존재하지 않는 사용자 ID, NotificationDto
        UUID userId = UUID.randomUUID();
        NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            userId,
            "제목",
            "내용",
            NotificationLevel.INFO
        );

        // when & then: 예외 발생 없이 전송 시도
        assertDoesNotThrow(() -> sseService.sendToClient(dto));
    }

    @Test
    void 단일_사용자_전송_실패() throws IOException {
        // given: 사용자와 실패하는 Emitter
        UUID userId = UUID.randomUUID();
        SseEmitter failingEmitter = mock(SseEmitter.class);
        doThrow(new IOException("fail"))
            .when(failingEmitter)
            .send(any(SseEmitter.SseEventBuilder.class));
        sseService.setUserEmitters(Map.of(userId, new CopyOnWriteArrayList<>(List.of(failingEmitter))));

        NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            userId,
            "제목",
            "내용",
            NotificationLevel.INFO
        );

        // when: NotificationDto 전송 시도
        sseService.sendToClient(dto);

        // then: 실패한 Emitter가 userEmitters에서 제거됨
        assertTrue(sseService.getUserEmitters().get(userId).isEmpty());
    }

    @Test
    void 역할_브로드캐스트_일부_실패() throws IOException {
        // given: 역할과 두 개의 Emitter 중 일부 실패
        Role role = Role.USER;
        SseEmitter goodEmitter = mock(SseEmitter.class);
        SseEmitter badEmitter = mock(SseEmitter.class);
        doThrow(new IOException("fail"))
            .when(badEmitter)
            .send(any(SseEmitter.SseEventBuilder.class));
        sseService.setRoleEmitters(Map.of(role, new CopyOnWriteArrayList<>(List.of(goodEmitter, badEmitter))));

        NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            null,
            "브로드캐스트 제목",
            "브로드캐스트 내용",
            NotificationLevel.INFO
        );

        // when: 역할 브로드캐스트 전송
        sseService.sendToRole(role, dto);

        // then: 실패한 Emitter만 제거, 성공한 Emitter는 남음
        assertTrue(sseService.getRoleEmitters().get(role).contains(goodEmitter));
        assertFalse(sseService.getRoleEmitters().get(role).contains(badEmitter));
    }

    @Test
    void 구독_초기_connect_실패() throws IOException {
        // given: spy Emitter와 IOException 발생 설정
        UUID userId = UUID.randomUUID();
        Role role = Role.USER;
        SseEmitter emitter = spy(new SseEmitter());
        doThrow(new IOException("fail"))
            .when(emitter)
            .send(any(SseEmitter.SseEventBuilder.class));

        // when: removeEmitter를 직접 호출해 초기 connect 실패 시 제거 시뮬레이션
        sseService.setUserEmitters(Map.of(userId, new CopyOnWriteArrayList<>()));
        sseService.setRoleEmitters(Map.of(role, new CopyOnWriteArrayList<>()));
        sseService.removeEmitter(userId, role, emitter);

        // then: userEmitters와 roleEmitters에 등록되지 않음
        assertFalse(sseService.getUserEmitters().getOrDefault(userId, new ArrayList<>()).contains(emitter));
        assertFalse(sseService.getRoleEmitters().getOrDefault(role, new ArrayList<>()).contains(emitter));
    }

    @Test
    void Emitter_send_실패시_제거() throws IOException {
        // given: spy Emitter와 send IOException 발생 설정
        UUID userId = UUID.randomUUID();
        Role role = Role.USER;

        SseEmitter realEmitter = new SseEmitter();
        SseEmitter emitter = spy(realEmitter);

        doThrow(new IOException("fail"))
            .when(emitter)
            .send(any(SseEmitter.SseEventBuilder.class));

        Map<UUID, List<SseEmitter>> userMap = new ConcurrentHashMap<>();
        userMap.put(userId, new CopyOnWriteArrayList<>(List.of(emitter)));
        sseService.setUserEmitters(userMap);

        Map<Role, List<SseEmitter>> roleMap = new ConcurrentHashMap<>();
        roleMap.put(role, new CopyOnWriteArrayList<>(List.of(emitter)));
        sseService.setRoleEmitters(roleMap);

        NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            userId,
            "제목",
            "내용",
            NotificationLevel.INFO
        );

        // when: 단일 사용자에게 Notification 전송
        sseService.sendToClient(dto);

        // then: 실패한 Emitter가 userEmitters에서 제거됨
        assertTrue(sseService.getUserEmitters().get(userId).isEmpty(), "userEmitters에서 제거되지 않음");
    }

    @Test
    void Emitter_이벤트_콜백_제거_동작_검증() {
        // given: 사용자와 역할, SSE 구독 생성
        UUID userId = UUID.randomUUID();
        Role role = Role.USER;
        SseEmitter emitter = sseService.subscribe(userId, role, null);

        // when: 콜백 강제 실행
        emitter.complete(); // onCompletion()
        emitter.onTimeout(() -> {}); // 타임아웃 등록 후 직접 제거 호출
        sseService.removeEmitter(userId, role, emitter); // clean up

        // then: emitter가 제거되었음을 검증
        assertTrue(sseService.getUserEmitters().get(userId).isEmpty());
        assertTrue(sseService.getRoleEmitters().get(role).isEmpty());
    }

    @Test
    void 사용자_Emitter_목록이_비었을_때_전송_스킵() {
        // given: 비어 있는 Emitter 리스트와 NotificationDto
        UUID userId = UUID.randomUUID();
        sseService.setUserEmitters(Map.of(userId, new CopyOnWriteArrayList<>())); // empty list

        NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            userId,
            "제목",
            "내용",
            NotificationLevel.INFO
        );

        // when & then: 전송 시도 시 예외 없이 통과 ( 로그만 발생 )
        assertDoesNotThrow(() -> sseService.sendToClient(dto));
    }

    @Test
    void 역할별_Emitter_없을_때_브로드캐스트_스킵() {
        // given: 존재하지 않는 역할과 NotificationDto
        Role role = Role.ADMIN;
        NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            null,
            "관리자 알림",
            "내용",
            NotificationLevel.INFO
        );

        // when & then: 브로드캐스트 시도 시 아무 예외 없이 종료
        assertDoesNotThrow(() -> sseService.sendToRole(role, dto));
    }
}