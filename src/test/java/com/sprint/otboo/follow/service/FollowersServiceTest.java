package com.sprint.otboo.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import com.sprint.otboo.follow.repository.FollowQueryRepository;
import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("팔로워 목록 조회 서비스 테스트")
class FollowersServiceTest {

    private final FollowRepository followRepository = Mockito.mock(FollowRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final FollowQueryRepository queryRepository = Mockito.mock(FollowQueryRepository.class);
    ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

    private final FollowService service = new FollowServiceImpl(
        followRepository, userRepository, queryRepository,eventPublisher
    );

    // 첫 페이지: limit개 + hasNext=true → nextCursor/nextIdAfter 세팅
    @Test
    void 첫_페이지() {
        UUID me = UUID.randomUUID();
        int limit = 2;

        // FollowListItemResponse는 (followId, followeeSummary, followerSummary, createdAt)
        FollowListItemResponse r1 = new FollowListItemResponse(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            /* followee */ null, /* follower */ null,
            Instant.parse("2025-10-16T03:00:00Z")
        );
        FollowListItemResponse r2 = new FollowListItemResponse(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            null, null, Instant.parse("2025-10-16T03:00:00Z")
        );
        FollowListItemResponse extra = new FollowListItemResponse(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            null, null, Instant.parse("2025-10-16T02:59:00Z")
        );

        when(queryRepository.findFollowersPage(eq(me), isNull(), isNull(), eq(limit + 1), isNull()))
            .thenReturn(List.of(r1, r2, extra));
        when(queryRepository.countFollowers(eq(me), isNull())).thenReturn(3L);

        CursorPageResponse<FollowListItemResponse> page =
            service.getFollowers(me, null, null, limit, null);

        assertThat(page.data()).hasSize(2);
        assertThat(page.hasNext()).isTrue();
        // 마지막 요소의 createdAt 기반 커서 + 마지막 요소의 followId 기반 idAfter
        assertThat(page.nextCursor()).isEqualTo("2025-10-16T03:00:00Z");
        assertThat(page.nextIdAfter()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(page.totalCount()).isEqualTo(3L);
        assertThat(page.sortBy()).isEqualTo("createdAt");
        assertThat(page.sortDirection()).isEqualTo("DESCENDING");
    }

    // 마지막 페이지: hasNext=false → nextCursor/nextIdAfter=null
    @Test
    void 마지막_페이지() {
        UUID me = UUID.randomUUID();
        int limit = 2;

        FollowListItemResponse r1 = new FollowListItemResponse(
            UUID.randomUUID(), null, null, Instant.parse("2025-10-16T02:59:00Z")
        );
        FollowListItemResponse r2 = new FollowListItemResponse(
            UUID.randomUUID(), null, null, Instant.parse("2025-10-16T02:58:00Z")
        );

        when(queryRepository.findFollowersPage(eq(me), isNull(), isNull(), eq(limit + 1), isNull()))
            .thenReturn(List.of(r1, r2));
        when(queryRepository.countFollowers(eq(me), isNull())).thenReturn(2L);

        CursorPageResponse<FollowListItemResponse> page =
            service.getFollowers(me, null, null, limit, null);

        assertThat(page.data()).hasSize(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.nextIdAfter()).isNull();
        assertThat(page.totalCount()).isEqualTo(2L);
    }

    // 커서 조회: createdAt,id 타이브레이커로 다음 페이지 선택
    @Test
    void 커서_조회() {
        UUID me = UUID.randomUUID();
        int limit = 2;
        Instant ts = Instant.parse("2025-10-16T03:00:00Z");

        when(queryRepository.findFollowersPage(eq(me), eq(ts.toString()), isNull(), eq(limit + 1), isNull()))
            .thenReturn(List.of());
        when(queryRepository.countFollowers(eq(me), isNull())).thenReturn(0L);

        CursorPageResponse<FollowListItemResponse> page =
            service.getFollowers(me, ts.toString(), null, limit, null);

        assertThat(page.data()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    // limit 경계 보정: 0→1, 999→100
    @Test
    void limit_경계_보정() {
        UUID me = UUID.randomUUID();

        when(queryRepository.findFollowersPage(eq(me), isNull(), isNull(), eq(1 + 1), isNull()))
            .thenReturn(List.of());
        when(queryRepository.countFollowers(eq(me), isNull())).thenReturn(0L);

        service.getFollowers(me, null, null, 0, null);
        Mockito.verify(queryRepository).findFollowersPage(eq(me), isNull(), isNull(), eq(2), isNull());

        Mockito.reset(queryRepository);
        when(queryRepository.findFollowersPage(eq(me), isNull(), isNull(), eq(100 + 1), isNull()))
            .thenReturn(List.of());
        when(queryRepository.countFollowers(eq(me), isNull())).thenReturn(0L);

        service.getFollowers(me, null, null, 999, null);
        Mockito.verify(queryRepository).findFollowersPage(eq(me), isNull(), isNull(), eq(101), isNull());
    }

    // nameLike blank → null 정규화
    @Test
    void nameLike_blank_정규화() {
        UUID me = UUID.randomUUID();

        when(queryRepository.findFollowersPage(eq(me), isNull(), isNull(), Mockito.anyInt(), isNull()))
            .thenReturn(List.of());
        when(queryRepository.countFollowers(eq(me), isNull())).thenReturn(0L);

        service.getFollowers(me, null, null, 20, "   "); // blank
        Mockito.verify(queryRepository).findFollowersPage(eq(me), isNull(), isNull(), eq(21), isNull());
        Mockito.verify(queryRepository).countFollowers(eq(me), isNull());
    }
}
