package com.sprint.otboo.clothing.dto.request;

import java.util.List;

public record ClothesAttributeDefUpdateRequest(
    String name,
    List<String> selectableValues
) {

}
