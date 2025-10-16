package com.sprint.otboo.follow.service;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.dto.data.FollowSummaryDto;
import com.sprint.otboo.follow.entity.Follow;
import com.sprint.otboo.follow.event.FollowCreatedEvent;
import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FollowServiceImpl(FollowRepository followRepository, UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public FollowDto create(UUID followerId, UUID followeeId) {

        // 자기 자신 팔로우 금지
        if (followerId.equals(followeeId)) {
            throw new FollowException(ErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }

        // 사용자 존재 검증
        if (!userRepository.existsById(followerId) || !userRepository.existsById(followeeId)) {
            throw new FollowException(ErrorCode.USER_NOT_FOUND);
        }

        // 중복 팔로우 금지
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new FollowException(ErrorCode.FOLLOW_ALREADY_EXISTS);
        }

        Follow saved = followRepository.save(Follow.of(followerId, followeeId));
        eventPublisher.publishEvent(new FollowCreatedEvent(saved.getFollowerId(), saved.getFolloweeId()));
        return new FollowDto(saved.getId(), saved.getFollowerId(), saved.getFolloweeId());
    }

    @Override
    public FollowSummaryDto getMySummary(UUID userId) {

        long following = followRepository.countByFollowerId(userId);
        long follower  = followRepository.countByFolloweeId(userId);
        return new FollowSummaryDto(follower, following);
    }
}
