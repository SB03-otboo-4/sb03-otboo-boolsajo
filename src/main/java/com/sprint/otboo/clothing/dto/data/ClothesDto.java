package com.sprint.otboo.clothing.dto.data;

import com.sprint.otboo.clothing.entity.ClothesType;
import java.util.List;
import java.util.UUID;

/**
 * 의상 정보 응답 DTO
 * <p>의상 조회 시 반환되는 데이터 구조</p>
 */
public record ClothesDto(
    UUID id,
    UUID ownerId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeDto> attributes
) {

}
