package com.sprint.otboo.dm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.repository.DMRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("DM 목록 조회 서비스 테스트")
class DMServiceReadTest {

    DMRepository repository = Mockito.mock(DMRepository.class);
    DMService service = new DMServiceImpl(repository);

    @Test
    void 페이징_계산_검증_hasNext_true() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        // repo: limit+1(=3) 반환 → 서비스 limit=2 → hasNext=true
        List<DirectMessageDto> fetched = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            fetched.add(new DirectMessageDto(
                UUID.randomUUID(), me, "me", null, other, "other", null,
                "hello" + i, Instant.parse("2025-10-14T05:2" + i + ":40Z")
            ));
        }

        when(repository.findDmPageBetween(me, other, null, null, 3))
            .thenReturn(fetched);
        when(repository.countDmBetween(me, other)).thenReturn(100L);

        CursorPageResponse<DirectMessageDto> resp = service.getDms(me, other, null, null, 2);

        assertThat(resp.hasNext()).isTrue();
        assertThat(resp.data()).hasSize(2);
        assertThat(resp.nextCursor()).isNotNull();
        assertThat(resp.nextIdAfter()).isNotNull();
        assertThat(resp.totalCount()).isEqualTo(100L);
        assertThat(resp.sortBy()).isEqualTo("createdAt");
        assertThat(resp.sortDirection()).isEqualTo("DESCENDING");
    }

    @Test
    void 다음페이지_마지막_hasNext_false() {
        DMRepository repository = Mockito.mock(DMRepository.class);
        DMService service = new DMServiceImpl(repository);

        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        // limit=2, repo는 2개만 반환 → hasNext=false
        List<DirectMessageDto> fetched = List.of(
            new DirectMessageDto(UUID.randomUUID(), me, "me", null, other, "other", null, "m1",
                Instant.parse("2025-10-14T05:29:40Z")),
            new DirectMessageDto(UUID.randomUUID(), me, "me", null, other, "other", null, "m2",
                Instant.parse("2025-10-14T05:28:40Z"))
        );

        String cursor = "2025-10-14T05:29:40Z";
        UUID idAfter = UUID.randomUUID();

        when(repository.findDmPageBetween(me, other, cursor, idAfter, 3)) // limit+1
            .thenReturn(fetched);
        when(repository.countDmBetween(me, other)).thenReturn(42L);

        CursorPageResponse<DirectMessageDto> resp = service.getDms(me, other, cursor, idAfter, 2);

        assertThat(resp.hasNext()).isFalse();
        assertThat(resp.data()).hasSize(2);
        assertThat(resp.totalCount()).isEqualTo(42L);
        assertThat(resp.nextCursor()).isNull();
        assertThat(resp.nextIdAfter()).isNull();
    }

    @Test
    void limit_null_기본값_적용() {
        DMRepository repository = Mockito.mock(DMRepository.class);
        DMService service = new DMServiceImpl(repository);

        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        int defaultLimit = 20;

        List<DirectMessageDto> fetched = new ArrayList<>();
        for (int i = 0; i < defaultLimit + 1; i++) { // limit+1 반환 → hasNext=true
            fetched.add(new DirectMessageDto(
                UUID.randomUUID(), me, "me", null, other, "other", null,
                "m"+i, Instant.parse("2025-10-14T05:20:40Z")));
        }

        when(repository.findDmPageBetween(me, other, null, null, defaultLimit + 1))
            .thenReturn(fetched);
        when(repository.countDmBetween(me, other)).thenReturn(200L);

        CursorPageResponse<DirectMessageDto> resp = service.getDms(me, other, null, null, null);

        assertThat(resp.hasNext()).isTrue();
        assertThat(resp.data()).hasSize(defaultLimit);
        assertThat(resp.totalCount()).isEqualTo(200L);
    }
}
