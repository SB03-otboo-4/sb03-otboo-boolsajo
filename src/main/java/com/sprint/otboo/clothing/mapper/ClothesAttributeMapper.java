package com.sprint.otboo.clothing.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import java.util.UUID;
import org.mapstruct.Mapper;

/**
 * 의상 속성(ClothesAttribute) 엔티티와 DTO 간 변환을 담당하는 Mapper.
 */
@Mapper(componentModel = "spring")
public interface ClothesAttributeMapper {

    /**
     * 의상 속성 엔티티를 DTO로 변환.
     *
     * @param entity 의상 속성 엔티티
     * @return 의상 속성 DTO
     */
    ClothesAttributeDto toDto(ClothesAttribute entity);

    /**
     * 의상 속성 DTO와 의상 ID를 기반으로 엔티티 생성.
     *
     * @param dto 의상 속성 DTO
     * @param clothesId 의상 ID
     * @return 생성된 의상 속성 엔티티
     */
    default ClothesAttribute toEntity(ClothesAttributeDto dto, UUID clothesId) {
        // 내부 메서드 호출하여 엔티티 생성
        return buildClothesAttribute(clothesId, dto.definitionId(), dto.value());
    }

    /**
     * 의상 속성 엔티티 빌더
     *
     * @param clothesId 의상 ID
     * @param definitionId 속성 정의 ID
     * @param value 속성 값
     * @return 생성된 의상 속성 엔티티
     */
    private ClothesAttribute buildClothesAttribute(UUID clothesId, UUID definitionId, String value) {
        return ClothesAttribute.create(clothesId, definitionId, value);
    }
}