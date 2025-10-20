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
            String content = "첫 댓글";

            User commentAuthor =  UserFixture.create("댓글작성자", "author.png");
            User feedAuthor = UserFixture.create("피드작성자", "author.png");
            Weather weather = WeatherFixture.createWeatherWithDefault(
                WeatherLocationFixture.createLocationWithDefault()
            );

            Feed feed = FeedFixture.createWithId(feedAuthor.getId());
            ReflectionTestUtils.setField(feed, "author", feedAuthor);
            ReflectionTestUtils.setField(feed, "weather", weather);

            Comment comment = CommentFixture.create(commentAuthor, feed, content, Instant.now());
            CommentDto expected = new CommentDto(
                comment.getId(),
                comment.getCreatedAt(),
                feed.getId(),
                new AuthorDto(commentAuthor.getId(), "홍길동", "commenter.png"),
                content
            );

            given(userRepository.findById(commentAuthor.getId())).willReturn(Optional.of(commentAuthor));
            given(feedRepository.findById(feed.getId())).willReturn(Optional.of(feed));
            given(commentRepository.save(any(Comment.class))).willReturn(comment);
            given(commentMapper.toDto(comment)).willReturn(expected);

            // when
            CommentDto result = commentService.create(commentAuthor.getId(), feed.getId(), content);

            // then
            assertThat(result).isSameAs(expected);
            then(userRepository).should().findById(commentAuthor.getId());
            then(feedRepository).should().findById(feed.getId());
            then(commentRepository).should().save(any(Comment.class));
            then(commentMapper).should().toDto(comment);
            then(eventPublisher).should().publishEvent(new FeedCommentedEvent(
                feedAuthor.getId(), commentAuthor.getId(), comment.getId()
            ));
        }

        @Test
        void 작성자를_찾을_수_없으면_예외가_발생한다() {
            // given
            UUID authorId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();
            String content = "내용";

            given(userRepository.findById(authorId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.create(authorId, feedId, content))
                .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void 피드를_찾을_수_없으면_예외가_발생한다() {
            // given
            UUID authorId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();
            String content = "내용";

            given(userRepository.findById(authorId)).willReturn(
                Optional.of(User.builder().id(authorId).build()));
            given(feedRepository.findById(feedId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.create(authorId, feedId, content))
                .isInstanceOf(FeedNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 조회 테스트")
    class CommentReadTests {

        @Test
        void 댓글을_조회하면_최신순으로_댓글이_조회된다() {
            // given
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

            // when
            CursorPageResponse<CommentDto> result =
                commentService.getComments(feedId, null, null, limit);

            // then
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
            // given
            UUID feedId = UUID.randomUUID();
            int limit = 10;

            Feed feed = FeedFixture.createWithId(feedId);

            given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
            given(commentRepository.findByFeedId(feedId, null, null, limit))
                .willReturn(List.of());
            given(commentRepository.countByFeedId(feedId)).willReturn(0L);

            // when
            CursorPageResponse<CommentDto> result =
                commentService.getComments(feedId, null, null, limit);

            // then
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
