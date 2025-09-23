package com.sprint.otboo.clothing.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClothesAttributeDefMapper {

    @Mapping(target = "selectableValues", expression = "java(entity.getSelectValues() != null ? List.of(entity.getSelectValues().split(\",\")) : List.of())")
    ClothesAttributeDefDto toDto(ClothesAttributeDef entity);

}
