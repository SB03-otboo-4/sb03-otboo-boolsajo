package com.sprint.otboo.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.sprint.otboo.common.dto.CursorPageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("팔로잉 목록 조회 서비스 테스트")
class FollowingsServiceTest {

    private final FollowQueryRepository queryRepository = Mockito.mock(FollowQueryRepository.class);
    private final FollowService service = new FollowServiceImpl(queryRepository);

    // 첫 페이지: limit개 + hasNext=true → nextCursor/nextIdAfter 세팅
    @Test
    void 첫_페이지() {
        UUID me = UUID.randomUUID();
        int limit = 2;

        FollowingRow r1 = new FollowingRow(
            UUID.fromString("386cb145-63c6-4333-89c9-6245789c6671"),
            new UserSummaryResponse(UUID.fromString("947e5ff1-508a-4f72-94b1-990e206c692b"), "slinky", null),
            new UserSummaryResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:29:40Z")
        );
        FollowingRow r2 = new FollowingRow(
            UUID.fromString("93d3247e-5628-4fe7-a6da-93611e1ff732"),
            new UserSummaryResponse(UUID.fromString("e88dc0a5-b1aa-441d-8ea1-540129b1b78b"), "jessie", null),
            new UserSummaryResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:29:40Z")
        );
        FollowingRow extra = new FollowingRow(
            UUID.randomUUID(),
            new UserSummaryResponse(UUID.randomUUID(), "rex", null),
            new UserSummaryResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:28:00Z")
        );

        when(queryRepository.findFollowingPage(eq(me), isNull(), isNull(), eq(limit + 1), isNull()))
            .thenReturn(List.of(r1, r2, extra));
        when(queryRepository.countFollowing(eq(me), isNull())).thenReturn(3L);

        CursorPageResponse<FollowListItemResponse> page = service.getFollowings(me, null, null, limit, null);

        assertThat(page.data()).hasSize(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo("2025-10-14T05:29:40Z");
        assertThat(page.nextIdAfter()).isEqualTo("93d3247e-5628-4fe7-a6da-93611e1ff732");
        assertThat(page.totalCount()).isEqualTo(3L);
        assertThat(page.sortBy()).isEqualTo("createdAt");
        assertThat(page.sortDirection()).isEqualTo("DESCENDING");
    }

    // 마지막 페이지: hasNext=false, nextCursor=null
    @Test
    void 마지막_페이지() {
        UUID me = UUID.randomUUID();
        int limit = 2;

        FollowingRow r1 = new FollowingRow(UUID.randomUUID(),
            new UserSummaryResponse(UUID.randomUUID(), "a", null),
            new UserSummaryResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:28:00Z"));
        FollowingRow r2 = new FollowingRow(UUID.randomUUID(),
            new UserSummaryResponse(UUID.randomUUID(), "b", null),
            new UserSummaryResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:27:00Z"));

        when(queryRepository.findFollowingPage(eq(me), isNull(), isNull(), eq(limit + 1), isNull()))
            .thenReturn(List.of(r1, r2));
        when(queryRepository.countFollowing(eq(me), isNull())).thenReturn(2L);

        CursorPageResponse<FollowListItemResponse> page = service.getFollowings(me, null, null, limit, null);

        assertThat(page.data()).hasSize(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.nextIdAfter()).isNull();
        assertThat(page.totalCount()).isEqualTo(2L);
    }

    // 커서가 주어지면 createdAt,id 기준으로 다음 페이지 선택
    @Test
    void 커서_조회() {
        UUID me = UUID.randomUUID();
        int limit = 2;

        when(queryRepository.findFollowingPage(eq(me), eq("2025-10-14T05:29:40Z"), isNull(), eq(limit + 1), isNull()))
            .thenReturn(List.of());
        when(queryRepository.countFollowing(eq(me), isNull())).thenReturn(0L);

        CursorPageResponse<FollowListItemResponse> page = service.getFollowings(me, "2025-10-14T05:29:40Z", null, limit, null);

        assertThat(page.data()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }
}
