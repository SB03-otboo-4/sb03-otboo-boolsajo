package com.sprint.otboo.feed.repository;

import com.sprint.otboo.feed.entity.FeedLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedLikeRepository extends JpaRepository<FeedLike, UUID> {

    boolean existsByFeedIdAndUserId(UUID feedId, UUID userId);

    int deleteByFeedIdAndUserId(UUID feedId, UUID userId);
}
