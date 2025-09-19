package com.sprint.otboo.recommendation.controller;

import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.service.RecommendationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<RecommendationDto> getRecommendations(
        @RequestParam UUID userId,
        @RequestParam UUID weatherId
    ) {
        RecommendationDto dto = recommendationService.getRecommendation(userId, weatherId);
        return ResponseEntity.ok(dto);
    }

}
