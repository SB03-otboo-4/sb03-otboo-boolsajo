package com.sprint.otboo.follow.service;

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
            throw new IllegalArgumentException("Cannot follow self");
        }
        if (repository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new IllegalStateException("Already following");
        }
        Follow saved = repository.save(Follow.of(followerId, followeeId));
        return new FollowDto(saved.getId(), saved.getFollowerId(), saved.getFolloweeId());
    }
}
