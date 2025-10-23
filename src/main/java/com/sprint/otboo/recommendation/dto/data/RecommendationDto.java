package com.sprint.otboo.recommendation.dto.data;

import com.sprint.otboo.clothing.dto.data.OotdDto;
import java.util.List;
import java.util.UUID;

/**
 * 사용자의 특정 날씨에 대한 추천 의상 정보를 담는 DTO.
 * <p>
 * 하나의 RecommendationDto는 특정 날씨(id)와 사용자(userId)에 대한
 * 의상(clothes) 추천 리스트를 포함합니다.
 */
public record RecommendationDto(
    UUID weatherId,
    UUID userId,
    List<OotdDto> clothes
) {

}
