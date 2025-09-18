package com.sprint.otboo.recommendation.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.entity.Recommendation;
import com.sprint.otboo.recommendation.entity.RecommendationClothes;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {

    @Mapping(source = "weather.id", target = "weatherId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "clothes", expression = "java(toClothesDtos(entity.getRecommendationClothes()))")
    RecommendationDto toDto(Recommendation entity);

    default List<ClothesDto> toClothesDtos(List<RecommendationClothes> entities) {
        return entities.stream()
            .map(RecommendationClothes::getClothes)
            .map(this::toClothesDto)
            .toList();
    }

    ClothesDto toClothesDto(Clothes clothes);

}
