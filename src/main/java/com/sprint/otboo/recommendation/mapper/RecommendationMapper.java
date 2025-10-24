package com.sprint.otboo.recommendation.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeWithDefDto;
import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.entity.Recommendation;
import com.sprint.otboo.recommendation.entity.RecommendationClothes;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 의상 추천 엔티티와 DTO 간 변환을 담당하는 Mapper.
 *
 * <p>MapStruct를 사용하여 다음 변환을 수행합니다.
 * <ul>
 *   <li>Recommendation → RecommendationDto</li>
 *   <li>Clothes → OotdDto</li>
 *   <li>RecommendationClothes 리스트 → OotdDto 리스트</li>
 *   <li>ClothesAttribute → ClothesAttributeWithDefDto</li>
 * </ul>
 *
 * <p>RecommendationDto 내의 clothes 리스트 변환 시
 * RecommendationClothes → Clothes → OotdDto 순으로 변환
 */
@Mapper(componentModel = "spring")
public interface RecommendationMapper {

    /**
     * Recommendation 엔티티를 RecommendationDto로 변환
     *
     * <p>RecommendationDto의 {@code clothes} 필드에는
     * RecommendationClothes를 통해 변환된 OotdDto 리스트 주입
     *
     * @param entity Recommendation 엔티티
     * @return RecommendationDto
     */
    @Mapping(source = "weather.id", target = "weatherId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "clothes", expression = "java(toOotdDtos(entity.getRecommendationClothes()))")
    RecommendationDto toDto(Recommendation entity);

    /**
     * RecommendationClothes 리스트를 OotdDto 리스트로 변환
     *
     * @param entities RecommendationClothes 엔티티 리스트
     * @return OotdDto 리스트
     */
    default List<OotdDto> toOotdDtos(List<RecommendationClothes> entities) {
        return entities.stream()
            .map(RecommendationClothes::getClothes)
            .map(this::toOotdDto)
            .toList();
    }

    /**
     * Clothes 엔티티를 OotdDto로 변환
     *
     * @param clothes Clothes 엔티티
     * @return OotdDto
     */
    @Mapping(source = "id", target = "clothesId")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "imageUrl", target = "imageUrl")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "attributes", target = "attributes")
    OotdDto toOotdDto(Clothes clothes);

    /**
     * ClothesAttribute 리스트를 ClothesAttributeWithDefDto 리스트로 변환
     *
     * @param attributes ClothesAttribute 리스트
     * @return ClothesAttributeWithDefDto 리스트
     */
    default List<ClothesAttributeWithDefDto> toAttributeDtos(List<ClothesAttribute> attributes) {
        return attributes.stream()
            .map(this::toAttributeDto)
            .toList();
    }

    /**
     * ClothesAttribute 엔티티를 ClothesAttributeWithDefDto로 변환
     *
     * <p>MapStruct는 자동으로 내부 정의(definition) 필드를 매핑하지 못하므로
     * {@code @Mapping}으로 명시적으로 매핑
     *
     * @param attribute ClothesAttribute 엔티티
     * @return ClothesAttributeWithDefDto
     */
    @Mapping(source = "definition.id", target = "definitionId")
    @Mapping(source = "definition.name", target = "definitionName")
    @Mapping(source = "definition.selectValues", target = "selectableValues", qualifiedByName = "splitSelectValues")
    @Mapping(source = "value", target = "value")
    ClothesAttributeWithDefDto toAttributeDto(ClothesAttribute attribute);

    /**
     * selectValues 문자열을 List<String>으로 변환
     *
     * @param selectValues "LIGHT,MEDIUM,HEAVY" 형태 문자열
     * @return List<String> ["LIGHT", "MEDIUM", "HEAVY"]
     */
    @Named("splitSelectValues")
    default List<String> splitSelectValues(String selectValues) {
        if (selectValues == null || selectValues.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(selectValues.split(","));
    }
}