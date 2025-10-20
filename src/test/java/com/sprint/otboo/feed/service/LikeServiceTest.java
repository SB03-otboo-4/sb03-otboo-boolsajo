package com.sprint.otboo.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.entity.FeedLike;
import com.sprint.otboo.feed.event.FeedLikedEvent;
import com.sprint.otboo.feed.repository.FeedLikeRepository;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeService 테스트")
public class LikeServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FeedRepository feedRepository;
    @Mock
    private FeedLikeRepository feedLikeRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private LikeServiceImpl likeService;

    @Nested
    @DisplayName("피드 좋아요 등록 테스트")
    class FeedLikeCreateTests {

        @Test
        void 좋아요를_등록하면_likeCount가_1_증가한다() {
            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            User liker = User.builder().id(userId).build();
            Feed feed = Feed.builder()
                .id(feedId)
                .author(User.builder().id(UUID.randomUUID()).build())
                .content("hi")
                .likeCount(0L)
                .commentCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(liker));
            given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
            given(feedLikeRepository.existsByFeedIdAndUserId(feedId, userId)).willReturn(false);
            given(feedLikeRepository.save(any(FeedLike.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            likeService.addLike(feedId, userId);

            // then
            assertThat(feed.getLikeCount()).isEqualTo(1L);
            then(userRepository).should().findById(userId);
            then(feedRepository).should().findById(feedId);
            then(feedLikeRepository).should().existsByFeedIdAndUserId(feedId, userId);
            then(feedLikeRepository).should().save(any(FeedLike.class));
            then(eventPublisher).should().publishEvent(any(FeedLikedEvent.class));
            then(userRepository).shouldHaveNoMoreInteractions();
            then(feedRepository).shouldHaveNoMoreInteractions();
            then(feedLikeRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        void 피드가_존재하지_않으면_예외가_발생한다() {
            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            given(feedRepository.findById(feedId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> likeService.addLike(feedId, userId))
                .isInstanceOf(FeedNotFoundException.class)
                .hasMessageContaining("피드를 찾을 수 없습니다.");
            then(feedRepository).should().findById(feedId);
            then(feedLikeRepository).shouldHaveNoMoreInteractions();
            then(userRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        void 유저가_존재하지_않으면_예외가_발생한다() {
            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Feed feed = Feed.builder()
                .id(feedId)
                .author(User.builder().id(UUID.randomUUID()).build())
                .content("hi")
                .likeCount(0L)
                .commentCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> likeService.addLike(feedId, userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다.");
            then(feedRepository).should().findById(feedId);
            then(userRepository).should().findById(userId);
            then(feedLikeRepository).shouldHaveNoMoreInteractions();
            then(userRepository).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("피드 좋아요 삭제 테스트")
    class FeedLikeDeleteTests {

        @Test
        void 좋아요를_삭제하면_likeCount가_1_감소한다() {
            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            User liker = User.builder().id(userId).build();
            Feed feed = Feed.builder()
                .id(feedId)
                .author(User.builder().id(UUID.randomUUID()).build())
                .content("hi")
                .likeCount(1L)
                .commentCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
            given(userRepository.findById(userId)).willReturn(Optional.of(liker));
            given(feedLikeRepository.existsByFeedIdAndUserId(feedId, userId)).willReturn(true);
            given(feedLikeRepository.deleteByFeedIdAndUserId(feedId, userId)).willReturn(1);

            // when
            likeService.removeLike(feedId, userId);

            // then
            assertThat(feed.getLikeCount()).isEqualTo(0L);
            then(feedRepository).should().findById(feedId);
            then(userRepository).should().findById(userId);
            then(feedLikeRepository).should().existsByFeedIdAndUserId(feedId, userId);
            then(feedLikeRepository).should().deleteByFeedIdAndUserId(feedId, userId);
            then(feedRepository).shouldHaveNoMoreInteractions();
            then(userRepository).shouldHaveNoMoreInteractions();
            then(feedLikeRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        void 피드가_존재하지_않으면_예외가_발생한다() {
            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            given(feedRepository.findById(feedId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> likeService.removeLike(feedId, userId))
                .isInstanceOf(FeedNotFoundException.class)
                .hasMessageContaining("피드를 찾을 수 없습니다.");
            then(feedRepository).should().findById(feedId);
            then(feedLikeRepository).shouldHaveNoMoreInteractions();
            then(userRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        void 유저가_존재하지_않으면_예외가_발생한다() {
            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Feed feed = Feed.builder()
                .id(feedId)
                .author(User.builder().id(UUID.randomUUID()).build())
                .content("hi")
                .likeCount(1L)
                .commentCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> likeService.removeLike(feedId, userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다.");
            then(feedRepository).should().findById(feedId);
            then(userRepository).should().findById(userId);
            then(feedLikeRepository).shouldHaveNoMoreInteractions();
            then(userRepository).shouldHaveNoMoreInteractions();
        }
    }
}
