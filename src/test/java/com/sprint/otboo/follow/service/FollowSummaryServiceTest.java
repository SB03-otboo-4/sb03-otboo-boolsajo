package com.sprint.otboo.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.sprint.otboo.follow.dto.data.FollowSummaryDto;
import com.sprint.otboo.follow.repository.FollowQueryRepository;
import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.user.repository.UserRepository;
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
    void follower_following_카운트를_반환한다() {
        UUID me = UUID.randomUUID();
        given(followRepository.countByFolloweeId(me)).willReturn(3L); // 나를 팔로우하는 수
        given(followRepository.countByFollowerId(me)).willReturn(5L); // 내가 팔로우하는 수

        FollowSummaryDto dto = service.getMySummary(me);

        assertThat(dto.followerCount()).isEqualTo(3L);
        assertThat(dto.followingCount()).isEqualTo(5L);
    }
}
