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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final FeedLikeRepository feedLikeRepository;

    @Transactional
    @Override
    public void addLike(UUID feedId, UUID userId) {
        log.debug("[LikeServiceImpl] 좋아요 등록 요청: feedId={}, userId={}",
            feedId, userId);
        Feed feed = feedRepository.findById(feedId)
            .orElseThrow(() -> FeedNotFoundException.withId(feedId));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> UserNotFoundException.withId(userId));

        boolean exists = feedLikeRepository.existsByFeedIdAndUserId(feedId, userId);
        if (exists) {
            log.debug("[LikeServiceImpl] 좋아요가 이미 존재함: feedId={}, userId={}", feedId, userId);
            return;
        }
        feedLikeRepository.save(FeedLike.builder().feed(feed).user(user).build());
        feed.increaseLikeCount();

        log.debug("[LikeServiceImpl] 좋아요 등록 완료: feedId={}, userId={}, likeCount={}",
            feedId, userId, feed.getLikeCount());

    }
}
