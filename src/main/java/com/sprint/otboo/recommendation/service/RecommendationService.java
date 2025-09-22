package com.sprint.otboo.recommendation.service;

import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import java.util.UUID;

public interface RecommendationService {

    RecommendationDto getRecommendation(UUID userId, UUID weatherId);
}
