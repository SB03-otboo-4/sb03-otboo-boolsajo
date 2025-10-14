package com.sprint.otboo.follow.service;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.entity.Follow;
import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowServiceImpl(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Override
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
        return new FollowDto(saved.getId(), saved.getFollowerId(), saved.getFolloweeId());
    }
}
