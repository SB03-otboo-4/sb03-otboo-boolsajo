package com.sprint.otboo.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("팔로우 요약 정보 서비스 테스트")
class FollowSummaryServiceTest {

    FollowRepository followRepository = Mockito.mock(FollowRepository.class);
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    FollowService service = new FollowServiceImpl(followRepository, userRepository);

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
