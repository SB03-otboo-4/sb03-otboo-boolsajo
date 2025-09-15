package com.sprint.otboo.clothing.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeWithDefDto;
import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.feed.entity.FeedClothes;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface ClothesMapper {

    @Mapping(source = "clothes.id", target = "clothesId")
    @Mapping(source = "clothes.name", target = "name")
    @Mapping(source = "clothes.imageUrl", target = "imageUrl")
    @Mapping(source = "clothes.type", target = "type")
    @Mapping(source = "clothes.attributes", target = "attributes")
    OotdDto toOotdDto(FeedClothes feedClothes);

    @Mapping(source = "definition.id", target = "definitionId")
    @Mapping(source = "definition.name", target = "definitionName")
    @Mapping(target = "selectableValues", expression = "java(mapOptionsToSelectableValues(attribute))")
    @Mapping(source = "value", target = "value")
    ClothesAttributeWithDefDto toClothesAttributeWithDefDto(ClothesAttribute attribute);

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
}
