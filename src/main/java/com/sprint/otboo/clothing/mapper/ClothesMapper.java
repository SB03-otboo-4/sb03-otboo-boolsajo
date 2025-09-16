package com.sprint.otboo.clothing.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeWithDefDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.feed.entity.FeedClothes;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 의상 관련 엔티티와 DTO 간 변환을 담당하는 Mapper
 *
 * <p>MapStruct를 사용하여 다음 변환을 수행합니다.
 * <ul>
 *   <li>Clothes → ClothesDto</li>
 *   <li>ClothesAttribute → ClothesAttributeWithDefDto</li>
 *   <li>ClothesAttributeDef → ClothesAttributeDefDto</li>
 *   <li>FeedClothes → OotdDto</li>
 * </ul>
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface ClothesMapper {

    /**
     * FeedClothes 엔티티를 OotdDto로 변환
     *
     * @param feedClothes 피드용 의상 엔티티
     * @return OotdDto
     */
    @Mapping(source = "clothes.id", target = "clothesId")
    @Mapping(source = "clothes.name", target = "name")
    @Mapping(source = "clothes.imageUrl", target = "imageUrl")
    @Mapping(source = "clothes.type", target = "type")
    @Mapping(source = "clothes.attributes", target = "attributes")
    OotdDto toOotdDto(FeedClothes feedClothes);

    /**
     * ClothesAttribute 엔티티를 ClothesAttributeWithDefDto로 변환
     *
     * @param attribute 의상 속성 엔티티
     * @return ClothesAttributeWithDefDto
     */
    @Mapping(source = "definition.id", target = "definitionId")
    @Mapping(source = "definition.name", target = "definitionName")
    @Mapping(target = "selectableValues", expression = "java(mapOptionsToSelectableValues(attribute))")
    @Mapping(source = "value", target = "value")
    ClothesAttributeWithDefDto toClothesAttributeWithDefDto(ClothesAttribute attribute);

    /**
     * 의상 속성 엔티티의 선택값 문자열을 리스트로 변환
     *
     * @param attribute 의상 속성 엔티티
     * @return 선택값 리스트
     */
    default java.util.List<String> mapOptionsToSelectableValues(ClothesAttribute attribute) {
        if (attribute == null || attribute.getDefinition() == null) {
            return java.util.List.of();
        }
        String raw = attribute.getDefinition().getSelectValues();
        if (raw == null || raw.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    /**
     * 의상 엔티티를 DTO로 변환.
     *
     * @param clothes 의상 엔티티
     * @return 의상 DTO
     */
    @Mapping(target = "ownerId", source = "user.id")
    @Mapping(target = "attributes", source = "attributes")
    ClothesDto toDto(Clothes clothes);

    /**
     * 선택값 문자열을 리스트로 변환
     *
     * @param raw 콤마 구분 문자열
     * @return 선택값 리스트
     */
    default List<String> mapSelectValues(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .toList();
    }

    /**
     * ClothesAttributeDef 엔티티를 DTO로 변환
     *
     * @param entity 의상 속성 정의 엔티티
     * @return ClothesAttributeDefDto
     */
    default ClothesAttributeDefDto toClothesAttributeDefDto(ClothesAttributeDef entity) {
        if (entity == null) return null;
        return new ClothesAttributeDefDto(
            entity.getId(),
            entity.getName(),
            mapSelectValues(entity.getSelectValues()),
            entity.getCreatedAt()
        );
    }
}