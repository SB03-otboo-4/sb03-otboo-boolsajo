package com.sprint.otboo.follow.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.entity.Follow;
import com.sprint.otboo.follow.repository.FollowRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("언팔로우 서비스 테스트")
class UnfollowServiceTest {

    FollowRepository followRepository = Mockito.mock(FollowRepository.class);
    FollowService service = new FollowServiceImpl(followRepository, null);

    @Test
    @DisplayName("팔로우 관계가 존재하면 정상적으로 언팔로우된다")
    void 팔로우_관계가_존재하면_언팔로우_성공() {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        Follow follow = new Follow(followerId, followeeId);

        when(followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId))
            .thenReturn(Optional.of(follow));

        service.unfollow(followerId, followeeId);

        verify(followRepository).delete(follow);
    }

    @Test
    @DisplayName("팔로우 관계가 없으면 예외가 발생한다")
    void 팔로우_관계가_없으면_예외_발생() {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();

        when(followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unfollow(followerId, followeeId))
            .isInstanceOf(FollowException.class)
            .hasMessageContaining(ErrorCode.NOT_FOLLOWING.getMessage());
    }
}
