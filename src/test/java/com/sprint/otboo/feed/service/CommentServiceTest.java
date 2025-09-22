package com.sprint.otboo.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.feed.entity.Comment;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.CommentMapper;
import com.sprint.otboo.feed.repository.CommentRepository;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.fixture.CommentFixture;
import com.sprint.otboo.fixture.UserFixture;
import com.sprint.otboo.fixture.FeedFixture;
import com.sprint.otboo.user.dto.data.AuthorDto;
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

    @InjectMocks
    CommentServiceImpl commentService;

    @Nested
    @DisplayName("댓글 등록 테스트")
    class CreateTests {

        @Test
        void 댓글을_등록하면_DTO가_반환된다() {
            // Given
            UUID authorId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            String content = "첫 댓글";

            User author = UserFixture.create(authorId, "홍길동", "profile.png");

            Feed feed = FeedFixture.createWithId(feedId);

            Comment saved = CommentFixture.create(commentId, author, feed, "댓글", Instant.now());

            CommentDto expected = new CommentDto(
                saved.getId(),
                saved.getCreatedAt(),
                feedId,
                new AuthorDto(authorId, "홍길동", "profile.png"),
                content
            );

            given(userRepository.findById(authorId)).willReturn(Optional.of(author));
            given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
            given(commentRepository.save(any(Comment.class))).willReturn(saved);
            given(commentMapper.toDto(saved)).willReturn(expected);

            // When
            CommentDto result = commentService.create(authorId, feedId, content);

            // Then
            assertThat(result).isSameAs(expected);
            then(userRepository).should().findById(authorId);
            then(feedRepository).should().findById(feedId);
            then(commentRepository).should().save(any(Comment.class));
            then(commentMapper).should().toDto(saved);
            then(userRepository).shouldHaveNoMoreInteractions();
            then(feedRepository).shouldHaveNoMoreInteractions();
            then(commentRepository).shouldHaveNoMoreInteractions();
            then(commentMapper).shouldHaveNoMoreInteractions();
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
}
