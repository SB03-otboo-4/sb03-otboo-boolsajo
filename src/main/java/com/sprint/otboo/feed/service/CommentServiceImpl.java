package com.sprint.otboo.feed.service;

import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.feed.entity.Comment;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.CommentMapper;
import com.sprint.otboo.feed.repository.CommentRepository;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    @Transactional
    public CommentDto create(UUID authorId, UUID feedId, String content) {
        log.debug("[CommentServiceImpl] 댓글 생성 시작: authorId={}, feedId={}, content={}",
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

        return commentMapper.toDto(saved);
    }
}
