package com.sprint.otboo.dm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sprint.otboo.common.exception.dm.DMException;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.entity.DM;
import com.sprint.otboo.dm.repository.DMRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DM 전송 서비스 테스트")
class DMServiceSendTest {

    DMRepository repository = mock(DMRepository.class);
    DMService service = new DMServiceImpl(repository);

    @Test
    void 전송_성공() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        DM saved = DM.builder()
            .senderId(me)
            .receiverId(other)
            .content("hello")
            .build();
        // id/createdAt 세팅 시뮬레이션
        saved.setId(UUID.randomUUID());
        saved.setCreatedAt(Instant.parse("2025-10-14T05:30:00Z"));

        when(repository.save(any(DM.class))).thenReturn(saved);

        DirectMessageDto dto = service.sendDm(me, other, "hello");

        assertThat(dto.id()).isEqualTo(saved.getId());
        assertThat(dto.senderId()).isEqualTo(me);
        assertThat(dto.receiverId()).isEqualTo(other);
        assertThat(dto.content()).isEqualTo("hello");
        assertThat(dto.createdAt()).isEqualTo(saved.getCreatedAt());
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
}
