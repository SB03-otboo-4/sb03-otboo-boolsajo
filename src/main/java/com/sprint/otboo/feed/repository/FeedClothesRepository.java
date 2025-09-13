package com.sprint.otboo.feed.repository;

import com.sprint.otboo.feed.entity.FeedClothes;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedClothesRepository extends JpaRepository<FeedClothes, UUID> {

    boolean existsByFeed_IdAndClothes_Id(UUID feedId, UUID clothesId);
}