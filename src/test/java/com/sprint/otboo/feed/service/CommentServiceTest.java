package com.sprint.otboo.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.feed.entity.Comment;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.event.FeedCommentedEvent;
import com.sprint.otboo.feed.mapper.CommentMapper;
import com.sprint.otboo.feed.repository.CommentRepository;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.fixture.CommentFixture;
import com.sprint.otboo.fixture.UserFixture;
import com.sprint.otboo.fixture.FeedFixture;
import com.sprint.otboo.fixture.WeatherFixture;
import com.sprint.otboo.fixture.WeatherLocationFixture;
import com.sprint.otboo.user.dto.data.AuthorDto;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.weather.entity.Weather;
import java.time.Instant;
import java.util.List;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 테스트")
public class CommentServiceTest {

    @Mock
    CommentRepository commentRepository;
    @Mock
    CommentMapper commentMapper;
    @Mock
    UserRepository userRepository;
    @Mock
    FeedRepository feedRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    CommentServiceImpl commentService;

    @Nested
    @DisplayName("댓글 등록 테스트")
    class CommentCreateTests {

        @Test
        void 댓글을_등록하면_DTO가_반환된다() {
            // given
            UUID commentAuthorId = UUID.randomUUID();
            UUID feedAuthorId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            String content = "첫 댓글";

            User commentAuthor = UserFixture.create(commentAuthorId, "홍길동", "commenter.png");
            User feedAuthor = UserFixture.create(feedAuthorId, "피드작성자", "author.png");
            Weather weather = WeatherFixture.createWeatherWithDefault(
                WeatherLocationFixture.createLocationWithDefault()
            );

            Feed feed = FeedFixture.createWithId(feedId);
            ReflectionTestUtils.setField(feed, "author", feedAuthor);
            ReflectionTestUtils.setField(feed, "weather", weather);

            Comment saved = CommentFixture.create(commentId, commentAuthor, feed, content, Instant.now());
            CommentDto expected = new CommentDto(
                saved.getId(),
                saved.getCreatedAt(),
                feedId,
                new AuthorDto(commentAuthorId, "홍길동", "commenter.png"),
                content
            );

            given(userRepository.findById(commentAuthorId)).willReturn(Optional.of(commentAuthor));
            given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
            given(commentRepository.save(any(Comment.class))).willReturn(saved);
            given(commentMapper.toDto(saved)).willReturn(expected);

            // when
            CommentDto result = commentService.create(commentAuthorId, feedId, content);

            // then
            assertThat(result).isSameAs(expected);
            then(userRepository).should().findById(commentAuthorId);
            then(feedRepository).should().findById(feedId);
            then(commentRepository).should().save(any(Comment.class));
            then(commentMapper).should().toDto(saved);
            then(eventPublisher).should().publishEvent(new FeedCommentedEvent(
                feedAuthorId, commentAuthorId, commentId
            ));
        }

        @Test
        void 작성자를_찾을_수_없으면_예외가_발생한다() {
            // Given
            UUID authorId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();
            String content = "내용";

            given(userRepository.findById(authorId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentService.create(authorId, feedId, content))
                .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void 피드를_찾을_수_없으면_예외가_발생한다() {
            // Given
            UUID authorId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();
            String content = "내용";

            given(userRepository.findById(authorId)).willReturn(
                Optional.of(User.builder().id(authorId).build()));
            given(feedRepository.findById(feedId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> commentService.create(authorId, feedId, content))
                .isInstanceOf(FeedNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 조회 테스트")
    class CommentReadTests {

        @Test
        void 댓글을_조회하면_최신순으로_댓글이_조회된다() {
            // Given
            UUID feedId = UUID.randomUUID();
            int limit = 2;

            Instant t3 = Instant.now();
            Instant t2 = t3.minusSeconds(10);
            Instant t1 = t3.minusSeconds(20);

            User author = UserFixture.create(UUID.randomUUID(), "홍길동", "profile.png");
            Feed feed = FeedFixture.createWithId(feedId);

            Comment c1 = CommentFixture.create(UUID.randomUUID(), author, feed, "첫 댓글", t3);
            Comment c2 = CommentFixture.create(UUID.randomUUID(), author, feed, "둘째 댓글", t2);
            Comment c3 = CommentFixture.create(UUID.randomUUID(), author, feed, "셋째 댓글", t1);

            CommentDto d1 = new CommentDto(
                c1.getId(), c1.getCreatedAt(), feedId,
                new AuthorDto(author.getId(), "홍길동", "profile.png"),
                "첫 댓글"
            );
            CommentDto d2 = new CommentDto(
                c2.getId(), c2.getCreatedAt(), feedId,
                new AuthorDto(author.getId(), "홍길동", "profile.png"),
                "둘째 댓글"
            );

            given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

            given(commentRepository.findByFeedId(feedId, null, null, limit))
                .willReturn(List.of(c1, c2, c3));

            given(commentRepository.countByFeedId(feedId)).willReturn(3L);

            given(commentMapper.toDto(c1)).willReturn(d1);
            given(commentMapper.toDto(c2)).willReturn(d2);

            // When
            CursorPageResponse<CommentDto> result =
                commentService.getComments(feedId, null, null, limit);

            // Then
            assertThat(result.data()).containsExactly(d1, d2);
            assertThat(result.hasNext()).isTrue();

            assertThat(result.nextCursor()).isEqualTo(c2.getCreatedAt().toString());
            assertThat(result.nextIdAfter()).isEqualTo(c2.getId().toString());

            then(feedRepository).should().findById(feedId);
            then(commentRepository).should().findByFeedId(feedId, null, null, limit);
            then(commentRepository).should().countByFeedId(feedId);
            then(commentMapper).should().toDto(c1);
            then(commentMapper).should().toDto(c2);
            then(commentRepository).shouldHaveNoMoreInteractions();
            then(commentMapper).shouldHaveNoMoreInteractions();
        }

        @Test
        void 댓글이_없으면_빈_결과를_반환한다() {
            // Given
            UUID feedId = UUID.randomUUID();
            int limit = 10;

            Feed feed = FeedFixture.createWithId(feedId);

            given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
            given(commentRepository.findByFeedId(feedId, null, null, limit))
                .willReturn(List.of());
            given(commentRepository.countByFeedId(feedId)).willReturn(0L);

            // When
            CursorPageResponse<CommentDto> result =
                commentService.getComments(feedId, null, null, limit);

            // Then
            assertThat(result.data()).isEmpty();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.nextIdAfter()).isNull();
            assertThat(result.hasNext()).isFalse();

            then(feedRepository).should().findById(feedId);
            then(commentRepository).should().findByFeedId(feedId, null, null, limit);
            then(commentRepository).should().countByFeedId(feedId);
            then(commentMapper).shouldHaveNoInteractions();
            then(commentRepository).shouldHaveNoMoreInteractions();
        }
    }
}
