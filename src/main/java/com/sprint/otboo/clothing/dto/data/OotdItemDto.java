package com.sprint.otboo.clothing.dto.data;

import com.sprint.otboo.clothing.entity.ClothesType;
import java.util.List;
import java.util.UUID;

public record OotdItemDto(
    UUID clothesId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeWithDefDto> attributes
) {

}