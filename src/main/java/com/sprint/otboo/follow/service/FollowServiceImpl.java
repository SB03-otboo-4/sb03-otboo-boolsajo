package com.sprint.otboo.follow.service;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.entity.Follow;
import com.sprint.otboo.follow.repository.FollowRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FollowServiceImpl implements FollowService {

    private final FollowRepository repository;

    public FollowServiceImpl(FollowRepository repository) {

        this.repository = repository;
    }

    @Override
    public FollowDto create(UUID followerId, UUID followeeId) {

        if (followerId.equals(followeeId)) {
            throw new FollowException(ErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }
        if (repository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new FollowException(ErrorCode.FOLLOW_ALREADY_EXISTS);
        }
        Follow saved = repository.save(Follow.of(followerId, followeeId));
        return new FollowDto(saved.getId(), saved.getFollowerId(), saved.getFolloweeId());
    }
}
