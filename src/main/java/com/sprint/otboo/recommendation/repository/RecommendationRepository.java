package com.sprint.otboo.recommendation.repository;


import com.sprint.otboo.recommendation.entity.Recommendation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    /**
     * 특정 사용자의 최근 추천 1건 조회
     *
     * @param userId 사용자 ID
     * @return 최신 추천 Optional
     */
    Optional<Recommendation> findTopByUser_IdOrderByCreatedAtDesc(UUID userId);

}
