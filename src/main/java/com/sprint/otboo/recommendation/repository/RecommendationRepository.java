package com.sprint.otboo.recommendation.repository;


import com.sprint.otboo.recommendation.entity.Recommendation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    Optional<Recommendation> findByUser_IdAndWeather_Id(UUID userId, UUID weatherId);

}
