package com.sprint.otboo.clothing.dto.data;

import java.util.List;

public record ScrapedClothesAttributeDto(
    String definitionName,
    List<String> selectableValues,
    String value
) {

}
