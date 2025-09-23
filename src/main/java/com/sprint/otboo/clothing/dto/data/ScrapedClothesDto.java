package com.sprint.otboo.clothing.dto.data;

import java.util.List;

public record ScrapedClothesDto(
    String name,
    String imageUrl,
    String type,
    List<ScrapedClothesAttributeDto> attributes
) {

}
