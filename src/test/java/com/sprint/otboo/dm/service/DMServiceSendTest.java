package com.sprint.otboo.dm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sprint.otboo.common.exception.dm.DMException;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.entity.DM;
import com.sprint.otboo.dm.repository.DMRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DM 전송 서비스 테스트")
class DMServiceSendTest {

    DMRepository repository = mock(DMRepository.class);
    DMService service = new DMServiceImpl(repository);

    private static void setField(Object target, String fieldName, Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field f = type.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException("리플렉션 주입 실패: " + fieldName, e);
            }
        }
        throw new IllegalArgumentException("필드를 찾을 수 없습니다: " + fieldName);
    }

    @Test
    void 전송_성공() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        DM saved = DM.builder()
            .senderId(me)
            .receiverId(other)
            .content("hello")
            .build();

        setField(saved, "id", UUID.randomUUID());
        setField(saved, "createdAt", Instant.parse("2025-10-14T05:30:00Z"));

        when(repository.save(any(DM.class))).thenReturn(saved);

        DirectMessageDto dto = service.sendDm(me, other, "hello");

        assertThat(dto.id()).isEqualTo(saved.getId());
        assertThat(dto.senderId()).isEqualTo(me);
        assertThat(dto.receiverId()).isEqualTo(other);
        assertThat(dto.content()).isEqualTo("hello");
        assertThat(dto.createdAt()).isEqualTo(Instant.parse("2025-10-14T05:30:00Z"));

        verify(repository, times(1)).save(any(DM.class));
    }

    @Test
    void 자기자신에게_전송_오류() {
        UUID me = UUID.randomUUID();
        assertThatThrownBy(() -> service.sendDm(me, me, "x"))
            .isInstanceOf(DMException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void 내용_빈값_오류() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        assertThatThrownBy(() -> service.sendDm(me, other, ""))
            .isInstanceOf(DMException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void 내용_공백만_오류() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        assertThatThrownBy(() -> service.sendDm(me, other, "   \n\t"))
            .isInstanceOf(DMException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void 저장_성공하지만_createdAt_null이면_now_세팅() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        DM saved = DM.builder()
            .senderId(me)
            .receiverId(other)
            .content("hello")
            .build();

        // id 는 넣고 createdAt 은 null 로 둔다.
        setField(saved, "id", UUID.randomUUID());
        setField(saved, "createdAt", null);

        when(repository.save(any(DM.class))).thenReturn(saved);

        Instant before = Instant.now();
        DirectMessageDto dto = service.sendDm(me, other, "hello");
        Instant after = Instant.now();

        assertThat(dto.createdAt()).isNotNull();
        // now() 기반이므로 대략 범위로 검증
        assertThat(dto.createdAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    void 저장중_DB오류는_DMException으로_랩핑() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        when(repository.save(any(DM.class))).thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.sendDm(me, other, "hello"))
            .isInstanceOf(DMException.class);
    }

    @Test
    void 저장중_알수없는_예외도_DMException으로_랩핑() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        when(repository.save(any(DM.class))).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.sendDm(me, other, "hello"))
            .isInstanceOf(DMException.class);
    }
}
