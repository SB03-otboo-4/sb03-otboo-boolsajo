package com.sprint.otboo.feed.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.feed.entity.Comment;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.event.FeedCommentedEvent;
import com.sprint.otboo.feed.mapper.CommentMapper;
import com.sprint.otboo.feed.repository.CommentRepository;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {


    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CommentDto create(UUID authorId, UUID feedId, String content) {
        log.debug("[CommentServiceImpl] 댓글 생성 시작: authorId={}, feedId={}, data={}",
            authorId, feedId, content);
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> UserNotFoundException.withId(authorId));

        Feed feed = feedRepository.findById(feedId)
            .orElseThrow(() -> FeedNotFoundException.withId(feedId));

        Comment entity = Comment.builder()
            .author(author)
            .feed(feed)
            .content(content)
            .build();

        Comment saved = commentRepository.save(entity);
        log.debug("[CommentServiceImpl] 댓글 생성 완료: commentId={}", saved.getId());

        eventPublisher.publishEvent(new FeedCommentedEvent(
            feed.getAuthor().getId(),
            author.getId(),
            saved.getId()
        ));

        return commentMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<CommentDto> getComments(UUID feedId, String cursor, UUID idAfter,
        int limit) {
        log.debug("[CommentServiceImpl] 댓글 조회 시작: feedId={}, cursor={}, idAfter={}, limit={}",
            feedId, cursor, idAfter, limit);
        feedRepository.findById(feedId)
            .orElseThrow(() -> FeedNotFoundException.withId(feedId));

        List<Comment> fetched = commentRepository.findByFeedId(feedId, cursor, idAfter, limit);

        boolean hasNext = fetched.size() > limit;
        List<Comment> rows = hasNext ? fetched.subList(0, limit) : fetched;
        log.debug("[CommentServiceImpl] fetched={}, rows={}, hasNext={}", fetched.size(),
            rows.size(), hasNext);

        long totalCount = commentRepository.countByFeedId(feedId);
        log.debug("[CommentServiceImpl] totalCount={}", totalCount);

        List<CommentDto> data = rows.stream()
            .map(commentMapper::toDto)
            .toList();

        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && !rows.isEmpty()) {
            Comment last = rows.get(rows.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId().toString();
        }

        String sortBy = "createdAt";
        String sortDirection = "DESCENDING";

        CursorPageResponse<CommentDto> response = new CursorPageResponse<>(
            data,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            sortBy,
            sortDirection
        );

        log.info(
            "[CommentServiceImpl] 댓글 조회 완료: nextCursor={}, nextIdAfter={}, hasNext={}, size={}",
            nextCursor, nextIdAfter, hasNext, rows.size());

        return response;
    }
}
