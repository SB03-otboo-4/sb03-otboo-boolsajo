package com.sprint.otboo.clothing.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
    @Mapping(target = "definitionId", source = "definition.id")
    ClothesAttributeDto toDto(ClothesAttribute entity);
}