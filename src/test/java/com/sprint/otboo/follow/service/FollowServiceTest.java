package com.sprint.otboo.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sprint.otboo.follow.dto.FollowDto;
import com.sprint.otboo.follow.entity.Follow;
import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.follow.service.FollowService;
import com.sprint.otboo.follow.service.FollowServiceImpl;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("팔로우 생성 서비스 테스트")
class FollowServiceTest {

    FollowRepository repository = Mockito.mock(FollowRepository.class);
    FollowService service = new FollowServiceImpl(repository);

    @Test
    @DisplayName("자기 자신은 팔로우할 수 없다")
    void 자기_자신은_팔로우할_수_없다() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.create(id, id))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이미 팔로우 중이면 예외를 던져야 한다")
    void 이미_팔로우_중이면_예외를_던져야_한다() {
        UUID follower = UUID.randomUUID();
        UUID followee = UUID.randomUUID();

        when(repository.existsByFollowerIdAndFolloweeId(follower, followee)).thenReturn(true);

        assertThatThrownBy(() -> service.create(follower, followee))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("정상적으로 팔로우를 생성해야 한다")
    void 정상적으로_팔로우를_생성해야_한다() {
        UUID follower = UUID.randomUUID();
        UUID followee = UUID.randomUUID();

        when(repository.existsByFollowerIdAndFolloweeId(follower, followee)).thenReturn(false);
        when(repository.save(any(Follow.class))).thenAnswer(inv -> {
            Follow f = inv.getArgument(0);
            return new Follow(f.getId(), f.getFollowerId(), f.getFolloweeId(), f.getCreatedAt());
        });

        FollowDto dto = service.create(follower, followee);

        assertThat(dto.followerId()).isEqualTo(follower);
        assertThat(dto.followeeId()).isEqualTo(followee);
        assertThat(dto.id()).isNotNull();
    }
}
