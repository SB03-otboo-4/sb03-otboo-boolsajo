package com.sprint.otboo.clothing.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClothesMapper {

    // Clothes -> ClothesDto
    @Mapping(target = "attributes", source = "attributes")
    ClothesDto toDto(Clothes clothes, List<ClothesAttributeDto> attributes);

    // ClothesAttribute -> ClothesAttributeDto
    ClothesAttributeDto toAttrDto(ClothesAttribute entity);

    // ClothesAttributeDto -> ClothesAttribute
    default ClothesAttribute toEntity(ClothesAttributeDto dto, UUID clothesId) {
        return ClothesAttribute.create(clothesId, dto.definitionId(), dto.value());
    }
}