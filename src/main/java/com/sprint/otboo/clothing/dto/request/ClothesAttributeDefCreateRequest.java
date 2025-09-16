package com.sprint.otboo.clothing.dto.request;

import java.util.List;

public record ClothesAttributeDefCreateRequest(
    String name,
    List<String> selectableValues
) {

}
