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
class FollowingListServiceTest {

    private final FollowQueryRepository queryRepository = Mockito.mock(FollowQueryRepository.class);
    private final FollowService service = new FollowServiceImpl(queryRepository);

    // 첫 페이지: size개 반환 + hasNext/nextCursor 계산 + totalCount 포함
    @Test
    void 첫_페이지() {
        UUID me = UUID.randomUUID();
        int size = 2;

        FollowResponse f1 = new FollowResponse(
            UUID.fromString("386cb145-63c6-4333-89c9-6245789c6671"),
            new FollowUserSimpleResponse(UUID.fromString("947e5ff1-508a-4f72-94b1-990e206c692b"), "slinky", null),
            new FollowUserSimpleResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:29:40Z")
        );
        FollowResponse f2 = new FollowResponse(
            UUID.fromString("93d3247e-5628-4fe7-a6da-93611e1ff732"),
            new FollowUserSimpleResponse(UUID.fromString("e88dc0a5-b1aa-441d-8ea1-540129b1b78b"), "jessie", null),
            new FollowUserSimpleResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:29:40Z")
        );
        FollowResponse extra = new FollowResponse(
            UUID.randomUUID(),
            new FollowUserSimpleResponse(UUID.randomUUID(), "rex", null),
            new FollowUserSimpleResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:28:00Z")
        );

        when(queryRepository.findFollowingPage(eq(me), isNull(), eq(size + 1)))
            .thenReturn(List.of(f1, f2, extra));
        when(queryRepository.countFollowing(me)).thenReturn(3L);

        CursorPageResponse<FollowResponse> page = service.getFollowing(me, null, size);

        assertThat(page.data()).hasSize(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isNotNull();
        assertThat(page.totalCount()).isEqualTo(3L);
        assertThat(page.sortBy()).isEqualTo("createdAt");
        assertThat(page.sortDirection()).isEqualTo("DESCENDING");
    }

    // 마지막 페이지: hasNext=false, nextCursor=null
    @Test
    void 마지막_페이지() {
        UUID me = UUID.randomUUID();
        int size = 2;

        FollowResponse f1 = new FollowResponse(
            UUID.randomUUID(),
            new FollowUserSimpleResponse(UUID.randomUUID(), "a", null),
            new FollowUserSimpleResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:28:00Z")
        );
        FollowResponse f2 = new FollowResponse(
            UUID.randomUUID(),
            new FollowUserSimpleResponse(UUID.randomUUID(), "b", null),
            new FollowUserSimpleResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:27:00Z")
        );

        when(queryRepository.findFollowingPage(eq(me), isNull(), eq(size + 1)))
            .thenReturn(List.of(f1, f2));
        when(queryRepository.countFollowing(me)).thenReturn(2L);

        CursorPageResponse<FollowResponse> page = service.getFollowing(me, null, size);

        assertThat(page.data()).hasSize(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.totalCount()).isEqualTo(2L);
    }

    // 커서가 주어지면 커서 이전(createdAt,id)부터 size개 조회
    @Test
    void 커서_조회() {
        UUID me = UUID.randomUUID();
        int size = 2;
        String cursor = "MTczOTUzNzc4MDAwMDs5M2QzMjQ3ZS01NjI4LTRmZTctYTZkYS05MzYxMWUxZmY3MzI";

        when(queryRepository.findFollowingPage(eq(me), any(), eq(size + 1)))
            .thenReturn(List.of());
        when(queryRepository.countFollowing(me)).thenReturn(0L);

        CursorPageResponse<FollowResponse> page = service.getFollowing(me, cursor, size);

        assertThat(page.data()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }
}
