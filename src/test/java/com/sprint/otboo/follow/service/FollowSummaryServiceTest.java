package com.sprint.otboo.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.follow.dto.response.FollowSummaryResponse;
import com.sprint.otboo.follow.entity.Follow;
import com.sprint.otboo.follow.repository.FollowQueryRepository;
import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("팔로우 요약 정보 서비스 테스트")
class FollowSummaryServiceTest {

    private final FollowRepository followRepository = Mockito.mock(FollowRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final FollowQueryRepository followQueryRepository = Mockito.mock(FollowQueryRepository.class);

    private final FollowService service =
        new FollowServiceImpl(followRepository, userRepository, followQueryRepository);

    @Test
    void 타겟과_뷰어가_같으면_팔로우_관계_조회_안함() {
        UUID me = UUID.randomUUID();
        given(followRepository.countByFolloweeId(me)).willReturn(3L);
        given(followRepository.countByFollowerId(me)).willReturn(5L);

        FollowSummaryResponse resp = service.getSummary(me, me);

        assertThat(resp.followeeId()).isEqualTo(me);
        assertThat(resp.followerCount()).isEqualTo(3L);
        assertThat(resp.followingCount()).isEqualTo(5L);
        assertThat(resp.followedByMe()).isFalse();
        assertThat(resp.followedByMeId()).isNull();
        assertThat(resp.followingMe()).isFalse();

        verify(followRepository, never()).findByFollowerIdAndFolloweeId(any(), any());
        verify(followRepository, never()).existsByFollowerIdAndFolloweeId(any(), any());
    }

    @Test
    void 내가_타겟을_팔로우_중이면_followedByMe_true() {
        UUID viewer = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        UUID relId = UUID.randomUUID();

        Follow follow = Mockito.mock(Follow.class);
        when(follow.getId()).thenReturn(relId);

        given(followRepository.countByFolloweeId(target)).willReturn(10L);
        given(followRepository.countByFollowerId(target)).willReturn(5L);
        given(followRepository.findByFollowerIdAndFolloweeId(viewer, target))
            .willReturn(Optional.of(follow));
        given(followRepository.existsByFollowerIdAndFolloweeId(target, viewer))
            .willReturn(false);

        FollowSummaryResponse resp = service.getSummary(target, viewer);

        assertThat(resp.followedByMe()).isTrue();
        assertThat(resp.followedByMeId()).isEqualTo(relId);
        assertThat(resp.followingMe()).isFalse();
    }

    @Test
    void 타겟이_나를_팔로우_중이면_followingMe_true() {
        UUID viewer = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        given(followRepository.countByFolloweeId(target)).willReturn(0L);
        given(followRepository.countByFollowerId(target)).willReturn(0L);
        given(followRepository.findByFollowerIdAndFolloweeId(viewer, target))
            .willReturn(Optional.empty());
        given(followRepository.existsByFollowerIdAndFolloweeId(target, viewer))
            .willReturn(true);

        FollowSummaryResponse resp = service.getSummary(target, viewer);

        assertThat(resp.followedByMe()).isFalse();
        assertThat(resp.followedByMeId()).isNull();
        assertThat(resp.followingMe()).isTrue();
    }

    @Test
    void 서로_팔로우_관계_없으면_false_반환() {
        UUID viewer = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        given(followRepository.countByFolloweeId(target)).willReturn(1L);
        given(followRepository.countByFollowerId(target)).willReturn(2L);
        given(followRepository.findByFollowerIdAndFolloweeId(viewer, target))
            .willReturn(Optional.empty());
        given(followRepository.existsByFollowerIdAndFolloweeId(target, viewer))
            .willReturn(false);

        FollowSummaryResponse resp = service.getSummary(target, viewer);

        assertThat(resp.followedByMe()).isFalse();
        assertThat(resp.followedByMeId()).isNull();
        assertThat(resp.followingMe()).isFalse();
    }

    @Test
    void 타겟과_뷰어가_같으면_상호관계_조회_없이_카운트만_반환() {
        UUID me = UUID.randomUUID();
        given(followRepository.countByFolloweeId(me)).willReturn(10L);
        given(followRepository.countByFollowerId(me)).willReturn(20L);

        FollowSummaryResponse resp = service.getSummary(me, me);

        assertThat(resp.followeeId()).isEqualTo(me);
        assertThat(resp.followerCount()).isEqualTo(10L);
        assertThat(resp.followingCount()).isEqualTo(20L);
        assertThat(resp.followedByMe()).isFalse();
        assertThat(resp.followedByMeId()).isNull();
        assertThat(resp.followingMe()).isFalse();

        // 타겟=뷰어면 관계 조회 호출 안 됨
        Mockito.verify(followRepository, Mockito.never())
            .findByFollowerIdAndFolloweeId(any(), any());
        Mockito.verify(followRepository, Mockito.never())
            .existsByFollowerIdAndFolloweeId(any(), any());
    }

    @Test
    void 타겟과_뷰어가_다르면_내가_대상_팔로우_O_대상이_나_X() {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        given(followRepository.countByFolloweeId(target)).willReturn(3L);
        given(followRepository.countByFollowerId(target)).willReturn(5L);

        UUID relId = UUID.randomUUID();
        Follow rel = Follow.builder().id(relId).followerId(viewer).followeeId(target).build();
        given(followRepository.findByFollowerIdAndFolloweeId(viewer, target))
            .willReturn(java.util.Optional.of(rel));
        given(followRepository.existsByFollowerIdAndFolloweeId(target, viewer))
            .willReturn(false);

        FollowSummaryResponse resp = service.getSummary(target, viewer);

        assertThat(resp.followedByMe()).isTrue();
        assertThat(resp.followedByMeId()).isEqualTo(relId);
        assertThat(resp.followingMe()).isFalse();
    }

    @Test
    void 타겟과_뷰어가_다르면_내가_대상_팔로우_X_대상이_나_O() {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        given(followRepository.countByFolloweeId(target)).willReturn(1L);
        given(followRepository.countByFollowerId(target)).willReturn(2L);

        given(followRepository.findByFollowerIdAndFolloweeId(viewer, target))
            .willReturn(java.util.Optional.empty());
        given(followRepository.existsByFollowerIdAndFolloweeId(target, viewer))
            .willReturn(true);

        FollowSummaryResponse resp = service.getSummary(target, viewer);

        assertThat(resp.followedByMe()).isFalse();
        assertThat(resp.followedByMeId()).isNull();
        assertThat(resp.followingMe()).isTrue();
    }

    @Test
    void 타겟과_뷰어가_다르면_서로_팔로우_없음() {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        given(followRepository.countByFolloweeId(target)).willReturn(0L);
        given(followRepository.countByFollowerId(target)).willReturn(0L);

        given(followRepository.findByFollowerIdAndFolloweeId(viewer, target))
            .willReturn(java.util.Optional.empty());
        given(followRepository.existsByFollowerIdAndFolloweeId(target, viewer))
            .willReturn(false);

        FollowSummaryResponse resp = service.getSummary(target, viewer);

        assertThat(resp.followedByMe()).isFalse();
        assertThat(resp.followedByMeId()).isNull();
        assertThat(resp.followingMe()).isFalse();
    }
}
