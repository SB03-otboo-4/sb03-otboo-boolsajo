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
    void 페이징_계산_검증() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

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
}
