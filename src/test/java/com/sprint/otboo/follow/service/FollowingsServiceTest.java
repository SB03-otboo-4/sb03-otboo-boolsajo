package com.sprint.otboo.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import com.sprint.otboo.follow.repository.FollowQueryRepository;
import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import com.sprint.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("팔로잉 목록 조회 서비스 테스트")
class FollowingsServiceTest {

    private final FollowRepository followRepository = Mockito.mock(FollowRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final FollowQueryRepository queryRepository = Mockito.mock(FollowQueryRepository.class);
    ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

    private final FollowService service = new FollowServiceImpl(
        followRepository,
        userRepository,
        queryRepository,
        eventPublisher
    );

    // 첫 페이지: limit개 + hasNext=true → nextCursor/nextIdAfter 세팅
    @Test
    void 첫_페이지() {
        UUID me = UUID.randomUUID();
        int limit = 2;

        FollowListItemResponse item1 = new FollowListItemResponse(
            UUID.fromString("386cb145-63c6-4333-89c9-6245789c6671"),
            new UserSummaryResponse(UUID.fromString("947e5ff1-508a-4f72-94b1-990e206c692b"), "slinky", null),
            new UserSummaryResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:29:40Z")
        );
        FollowListItemResponse item2 = new FollowListItemResponse(
            UUID.fromString("93d3247e-5628-4fe7-a6da-93611e1ff732"),
            new UserSummaryResponse(UUID.fromString("e88dc0a5-b1aa-441d-8ea1-540129b1b78b"), "jessie", null),
            new UserSummaryResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:29:40Z")
        );
        FollowListItemResponse extra = new FollowListItemResponse(
            UUID.randomUUID(),
            new UserSummaryResponse(UUID.randomUUID(), "rex", null),
            new UserSummaryResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:28:00Z")
        );

        when(queryRepository.findFollowingPage(eq(me), isNull(), isNull(), eq(limit + 1), isNull()))
            .thenReturn(List.of(item1, item2, extra));
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

        FollowListItemResponse item1 = new FollowListItemResponse(
            UUID.randomUUID(),
            new UserSummaryResponse(UUID.randomUUID(), "a", null),
            new UserSummaryResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:28:00Z")
        );
        FollowListItemResponse item2 = new FollowListItemResponse(
            UUID.randomUUID(),
            new UserSummaryResponse(UUID.randomUUID(), "b", null),
            new UserSummaryResponse(me, "buzz", "https://s3/..."),
            Instant.parse("2025-10-14T05:27:00Z")
        );

        when(queryRepository.findFollowingPage(eq(me), isNull(), isNull(), eq(limit + 1), isNull()))
            .thenReturn(List.of(item1, item2));
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

        CursorPageResponse<FollowListItemResponse> page = service.getFollowings(
            me, "2025-10-14T05:29:40Z", null, limit, null
        );

        assertThat(page.data()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    // cursor와 idAfter가 함께 올 때 createdAt 동률이면 id DESC 타이브레이커 적용 후 nextCursor/nextIdAfter가 올바르게 계산된다
    @Test
    void cursor와_idAfter가_함께_올_때_타이브레이커_적용_후_올바르게_계산된다() {
        UUID me = UUID.fromString("68e17953-f79f-4d4f-8839-b26054887d5f");
        int limit = 2;
        Instant ts = Instant.parse("2025-10-14T05:29:40Z");

        // 두 개는 같은 createdAt, id는 내림차순으로 정렬된다고 가정
        FollowListItemResponse r1 = new FollowListItemResponse(
            UUID.fromString("386cb145-63c6-4333-89c9-6245789c6671"),
            new UserSummaryResponse(UUID.randomUUID(), "slinky", null),
            new UserSummaryResponse(me, "buzz", "https://..."),
            ts
        );
        FollowListItemResponse r2 = new FollowListItemResponse(
            UUID.fromString("93d3247e-5628-4fe7-a6da-93611e1ff732"),
            new UserSummaryResponse(UUID.randomUUID(), "jessie", null),
            new UserSummaryResponse(me, "buzz", "https://..."),
            ts
        );
        // 다음 페이지가 더 있음 표시용 extra
        FollowListItemResponse extra = new FollowListItemResponse(
            UUID.randomUUID(),
            new UserSummaryResponse(UUID.randomUUID(), "rex", null),
            new UserSummaryResponse(me, "buzz", "https://..."),
            Instant.parse("2025-10-14T05:28:00Z")
        );

        // repository는 limit+1개 반환 (서비스가 hasNext 판단)
        when(queryRepository.findFollowingPage(eq(me), eq(ts.toString()), any(), eq(limit + 1), isNull()))
            .thenReturn(List.of(r1, r2, extra));
        when(queryRepository.countFollowing(eq(me), isNull())).thenReturn(3L);

        // 실행
        CursorPageResponse<FollowListItemResponse> page =
            service.getFollowings(me, ts.toString(), null, limit, null);

        // 검증: 페이지 사이즈, hasNext, nextCursor/nextIdAfter 계산
        assertThat(page.data()).hasSize(2);
        assertThat(page.hasNext()).isTrue();
        // 마지막 요소 기준
        assertThat(page.nextCursor()).isEqualTo(ts.toString());
        assertThat(page.nextIdAfter()).isEqualTo("93d3247e-5628-4fe7-a6da-93611e1ff732");
        assertThat(page.totalCount()).isEqualTo(3L);
    }

    @Test
    void followings_limit_보정() {
        UUID me = UUID.randomUUID();
        int pageSize = 20;

        when(queryRepository.findFollowingPage(eq(me), isNull(), isNull(), eq(pageSize + 1), isNull()))
            .thenReturn(java.util.Collections.nCopies(pageSize,
                new FollowListItemResponse(UUID.randomUUID(), null, null, Instant.parse("2025-10-16T03:00:00Z"))));
        when(queryRepository.countFollowing(eq(me), isNull())).thenReturn(20L);

        service.getFollowings(me, null, null, 0, null);     // <= 0 -> 20으로 보정
        service.getFollowings(me, null, null, 999, null);   // > 100 -> 20으로 보정

        Mockito.verify(queryRepository, Mockito.times(2))
            .findFollowingPage(eq(me), isNull(), isNull(), eq(21), isNull());
    }

    @Test
    void followings_hasNext_true_마지막요소_null_필드_처리() {
        UUID me = UUID.randomUUID();
        int limit = 2;

        FollowListItemResponse a = new FollowListItemResponse(
            UUID.randomUUID(), null, null, Instant.parse("2025-10-16T03:00:00Z"));
        FollowListItemResponse b_nulls = new FollowListItemResponse(
            null, null, null, null); // 페이지 마지막 요소의 id/createdAt 모두 null
        FollowListItemResponse extra = new FollowListItemResponse(
            UUID.randomUUID(), null, null, Instant.parse("2025-10-16T02:59:00Z"));

        when(queryRepository.findFollowingPage(eq(me), isNull(), isNull(), eq(limit + 1), isNull()))
            .thenReturn(java.util.List.of(a, b_nulls, extra));
        when(queryRepository.countFollowing(eq(me), isNull())).thenReturn(3L);

        CursorPageResponse<FollowListItemResponse> page =
            service.getFollowings(me, null, null, limit, null);

        assertThat(page.hasNext()).isTrue();
        // 마지막 요소의 createdAt/id가 null이므로 next들도 null이어야 함
        assertThat(page.nextCursor()).isNull();
        assertThat(page.nextIdAfter()).isNull();
    }
}
