package com.sprint.otboo.recommendation.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.entity.Recommendation;
import com.sprint.otboo.recommendation.entity.RecommendationClothes;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 의상 추천 엔티티와 DTO 간 변환을 담당하는 Mapper.
 *
 * <p>MapStruct를 사용하여 다음 변환을 수행합니다.
 * <ul>
 *   <li>Recommendation → RecommendationDto</li>
 *   <li>Clothes → ClothesDto</li>
 *   <li>ClothesAttribute → ClothesAttributeDto</li>
 * </ul>
 *
 * <p>RecommendationDto 내의 clothes 리스트 변환 시
 * RecommendationClothes → Clothes → ClothesDto 순으로 변환됩니다.
 */
@Mapper(componentModel = "spring")
public interface RecommendationMapper {

    /**
     * Recommendation 엔티티를 RecommendationDto로 변환
     *
     * @param entity Recommendation 엔티티
     * @return RecommendationDto
     */
    @Mapping(source = "weather.id", target = "weatherId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "clothes", expression = "java(toClothesDtos(entity.getRecommendationClothes()))")
    RecommendationDto toDto(Recommendation entity);

    /**
     * RecommendationClothes 리스트를 ClothesDto 리스트로 변환
     *
     * @param entities RecommendationClothes 엔티티 리스트
     * @return ClothesDto 리스트
     */
    default List<ClothesDto> toClothesDtos(List<RecommendationClothes> entities) {
        return entities.stream()
            .map(RecommendationClothes::getClothes)
            .map(this::toClothesDto)
            .toList();
    }

    @Mapping(source = "id", target = "id")
    @Mapping(source = "user.id", target = "ownerId")
    @Mapping(source = "attributes", target = "attributes")
    ClothesDto toClothesDto(Clothes clothes);

    @Mapping(source = "definition.id", target = "definitionId")
    @Mapping(source = "value", target = "value")
    ClothesAttributeDto toClothesAttributeDto(ClothesAttribute attribute);

}
