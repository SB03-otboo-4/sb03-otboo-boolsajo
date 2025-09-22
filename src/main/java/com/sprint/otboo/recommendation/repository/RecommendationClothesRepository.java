package com.sprint.otboo.recommendation.repository;

import com.sprint.otboo.recommendation.entity.RecommendationClothes;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationClothesRepository extends JpaRepository<RecommendationClothes, UUID> {

}
