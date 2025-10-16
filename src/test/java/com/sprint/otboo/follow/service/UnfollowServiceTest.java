package com.sprint.otboo.follow.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.entity.Follow;
import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("언팔로우 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class UnfollowServiceTest {

    @Mock FollowRepository followRepository;
    @Mock UserRepository userRepository;

    @InjectMocks FollowServiceImpl service;

    @Test
    @DisplayName("팔로우 관계가 존재하면 정상적으로 언팔로우된다")
    void 팔로우_관계가_존재하면_언팔로우_성공() {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        Follow follow = mock(Follow.class); // 생성자 문제 피하기 위해 mock 사용

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
            .hasMessageContaining(ErrorCode.FOLLOW_NOT_FOUND.getMessage());
    }
}
