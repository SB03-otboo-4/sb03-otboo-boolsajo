package com.sprint.otboo.clothing.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 의상 속성 정의 Mapper
 *
 * <p>ClothesAttributeDef 엔티티와 ClothesAttributeDefDto 간 변환을 담당합니다.
 */
@Mapper(componentModel = "spring")
public interface ClothesAttributeDefMapper {

    /**
     * 의상 속성 정의 엔티티를 DTO로 변환.
     *
     * @param entity 의상 속성 정의 엔티티
     * @return 의상 속성 정의 DTO
     */
    @Mapping(target = "selectableValues", expression = "java(entity.getSelectValues() != null ? List.of(entity.getSelectValues().split(\",\")) : List.of())")
    ClothesAttributeDefDto toDto(ClothesAttributeDef entity);

}
