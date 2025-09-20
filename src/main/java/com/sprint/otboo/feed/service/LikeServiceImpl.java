package com.sprint.otboo.feed.service;

import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.entity.FeedLike;
import com.sprint.otboo.feed.repository.FeedLikeRepository;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeServiceImpl implements LikeService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final FeedLikeRepository feedLikeRepository;

    @Transactional
    @Override
    public void addLike(UUID feedId, UUID userId) {
        Feed feed = feedRepository.findById(feedId)
            .orElseThrow(() -> FeedNotFoundException.withId(feedId));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> UserNotFoundException.withId(userId));

        try {
            feedLikeRepository.save(FeedLike.builder().feed(feed).user(user).build());
            feed.increaseLikeCount();
        } catch (DataIntegrityViolationException e) {
            log.debug("[FeedServiceImpl] feedLike가 이미 존재함: feedId={}, userId={}", feedId, userId);
        }
    }
}
