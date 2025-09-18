package com.sprint.otboo.recommendation.service;

import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.entity.Recommendation;
import com.sprint.otboo.recommendation.exception.RecommendationNotFoundException;
import com.sprint.otboo.recommendation.mapper.RecommendationMapper;
import com.sprint.otboo.recommendation.repository.RecommendationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationMapper recommendationMapper;

    @Override
    public RecommendationDto getRecommendation(UUID userId, UUID weatherId) {
        Recommendation recommendation = recommendationRepository
            .findByUser_IdAndWeather_Id(userId, weatherId)
            .orElseThrow(RecommendationNotFoundException::new);

        return recommendationMapper.toDto(recommendation);
    }
}