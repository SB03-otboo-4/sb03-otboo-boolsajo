package com.sprint.otboo.clothing.dto.request;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import java.util.List;

/**
 * 의상 수정 요청 DTO
 *
 * <p>사용자가 기존 의상을 수정할 때 전달하는 데이터 객체
 *
 * @param name 수정할 의상 이름
 * @param type 수정할 의상 타입
 * @param attributes 수정할 의상 속성 리스트
 */
public record ClothesUpdateRequest(
    String name,
    ClothesType type,
    List<ClothesAttributeDto> attributes
) {

}
