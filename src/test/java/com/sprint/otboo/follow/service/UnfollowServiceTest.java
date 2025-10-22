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
    @InjectMocks FollowServiceImpl service;

    @Test
    void 관계ID_존재하면_언팔로우_성공() {
        UUID followerId = UUID.randomUUID();
        UUID followId   = UUID.randomUUID();

        Follow follow = mock(Follow.class);
        when(followRepository.findById(followId)).thenReturn(Optional.of(follow));
        when(follow.getFollowerId()).thenReturn(followerId);

        service.unfollowById(followerId, followId);

        verify(followRepository).delete(follow);
    }

    @Test
    void 관계ID_없으면_예외_발생() {
        UUID followerId = UUID.randomUUID();
        UUID followId   = UUID.randomUUID();

        when(followRepository.findById(followId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unfollowById(followerId, followId))
            .isInstanceOf(FollowException.class)
            .hasMessageContaining(ErrorCode.FOLLOW_NOT_FOUND.getMessage());
    }

    @Test
    void 다른사람_관계면_예외_발생() {
        UUID me       = UUID.randomUUID();
        UUID other    = UUID.randomUUID();
        UUID followId = UUID.randomUUID();

        Follow follow = mock(Follow.class);
        when(followRepository.findById(followId)).thenReturn(Optional.of(follow));
        when(follow.getFollowerId()).thenReturn(other);

        assertThatThrownBy(() -> service.unfollowById(me, followId))
            .isInstanceOf(FollowException.class)
            // 구현에서 ACCESS_DENIED가 아니라 NOT_FOUND로 통일했으면 아래도 NOT_FOUND로 확인
            .hasMessageContaining(ErrorCode.FOLLOW_NOT_FOUND.getMessage());
    }
}
