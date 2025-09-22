package com.sprint.otboo.clothing.dto.data;

import java.util.UUID;

/**
 * 의상 속성 DTO
 * <p>의상의 세부 속성(색상, 사이즈 등)을 나타냄</p>
 */
public record ClothesAttributeDto(
    UUID definitionId,
    String value
) {

}
