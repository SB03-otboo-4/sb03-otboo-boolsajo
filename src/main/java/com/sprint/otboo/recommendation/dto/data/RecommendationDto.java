package com.sprint.otboo.recommendation.dto.data;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import java.util.List;
import java.util.UUID;

public record RecommendationDto(
    UUID weatherId,
    UUID userId,
    List<ClothesDto> clothes
) {

}
