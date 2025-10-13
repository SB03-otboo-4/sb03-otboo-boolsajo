package com.sprint.otboo.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.entity.Follow;
import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("팔로우 생성 서비스 테스트")
class FollowServiceTest {

    FollowRepository followRepository = Mockito.mock(FollowRepository.class);
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    FollowService service = new FollowServiceImpl(followRepository, userRepository);

    @Test
    @DisplayName("자기 자신은 팔로우할 수 없다")
    void 자기_자신은_팔로우할_수_없다() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.create(id, id))
            .isInstanceOf(FollowException.class)
            .hasMessageContaining(ErrorCode.FOLLOW_SELF_NOT_ALLOWED.getMessage())
            .extracting(ex -> ((FollowException) ex).getErrorCode())
            .isEqualTo(ErrorCode.FOLLOW_SELF_NOT_ALLOWED);
    }

    @Test
    @DisplayName("팔로워가 존재하지 않으면 USER_NOT_FOUND 예외가 발생해야 한다")
    void 팔로워가_존재하지_않으면_USER_NOT_FOUND() {
        UUID follower = UUID.randomUUID();
        UUID followee = UUID.randomUUID();

        when(userRepository.existsById(follower)).thenReturn(false);

        assertThatThrownBy(() -> service.create(follower, followee))
            .isInstanceOf(FollowException.class)
            .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage())
            .extracting(ex -> ((FollowException) ex).getErrorCode())
            .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("팔로이가 존재하지 않으면 USER_NOT_FOUND 예외가 발생해야 한다")
    void 팔로이가_존재하지_않으면_USER_NOT_FOUND() {
        UUID follower = UUID.randomUUID();
        UUID followee = UUID.randomUUID();

        when(userRepository.existsById(follower)).thenReturn(true);
        when(userRepository.existsById(followee)).thenReturn(false);

        assertThatThrownBy(() -> service.create(follower, followee))
            .isInstanceOf(FollowException.class)
            .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage())
            .extracting(ex -> ((FollowException) ex).getErrorCode())
            .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 팔로우 중이면 예외를 던져야 한다")
    void 이미_팔로우_중이면_예외를_던져야_한다() {
        UUID follower = UUID.randomUUID();
        UUID followee = UUID.randomUUID();

        when(userRepository.existsById(follower)).thenReturn(true);
        when(userRepository.existsById(followee)).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFolloweeId(follower, followee)).thenReturn(true);

        assertThatThrownBy(() -> service.create(follower, followee))
            .isInstanceOf(FollowException.class)
            .hasMessageContaining(ErrorCode.FOLLOW_ALREADY_EXISTS.getMessage())
            .extracting(ex -> ((FollowException) ex).getErrorCode())
            .isEqualTo(ErrorCode.FOLLOW_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("정상적으로 팔로우를 생성해야 한다")
    void 정상적으로_팔로우를_생성해야_한다() {
        UUID follower = UUID.randomUUID();
        UUID followee = UUID.randomUUID();

        when(userRepository.existsById(follower)).thenReturn(true);
        when(userRepository.existsById(followee)).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFolloweeId(follower, followee)).thenReturn(false);
        when(followRepository.save(any(Follow.class))).thenAnswer(inv -> {
            Follow f = inv.getArgument(0);
            return new Follow(f.getId(), f.getFollowerId(), f.getFolloweeId(), f.getCreatedAt());
        });

        FollowDto dto = service.create(follower, followee);

        assertThat(dto.followerId()).isEqualTo(follower);
        assertThat(dto.followeeId()).isEqualTo(followee);
        assertThat(dto.id()).isNotNull();
    }
}
