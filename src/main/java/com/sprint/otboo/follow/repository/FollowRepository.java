package com.sprint.otboo.follow.repository;

import com.sprint.otboo.follow.entity.Follow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
    long countByFollowerId(java.util.UUID followerId);
    long countByFolloweeId(java.util.UUID followeeId);
}
