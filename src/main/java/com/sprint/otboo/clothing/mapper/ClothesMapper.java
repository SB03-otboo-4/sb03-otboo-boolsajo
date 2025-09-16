package com.sprint.otboo.clothing.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.Clothes;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 의상(Clothes) 엔티티와 DTO 간 변환을 담당하는 Mapper.
 */
@Mapper(componentModel = "spring", uses = {ClothesAttributeMapper.class})
public interface ClothesMapper {

    /**
     * 의상 엔티티를 DTO로 변환.
     *
     * @param clothes 의상 엔티티
     * @return 의상 DTO
     */
    @Mapping(target = "ownerId", source = "user.id")
    @Mapping(target = "attributes", source = "attributes")
    ClothesDto toDto(Clothes clothes);
}