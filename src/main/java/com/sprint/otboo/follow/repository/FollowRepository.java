package com.sprint.otboo.follow.repository;

import com.sprint.otboo.follow.entity.Follow;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
    long countByFollowerId(UUID followerId);
    long countByFolloweeId(UUID followeeId);
    Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
}
