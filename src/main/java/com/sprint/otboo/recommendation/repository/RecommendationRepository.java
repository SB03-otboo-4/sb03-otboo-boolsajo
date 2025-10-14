package com.sprint.otboo.recommendation.repository;


import com.sprint.otboo.recommendation.entity.Recommendation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByUser_IdAndCreatedAtAfter(UUID userId, LocalDateTime after);
}
