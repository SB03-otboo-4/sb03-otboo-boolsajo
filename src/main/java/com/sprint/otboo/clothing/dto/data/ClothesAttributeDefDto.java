package com.sprint.otboo.clothing.dto.data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 의상 속성 정의 DTO
 *
 * <p>ClothesAttributeDef 엔티티를 외부로 반환할 때 사용되는 데이터 전송 객체
 *
 * @param id 속성 정의 고유 ID
 * @param name 속성 이름
 * @param selectableValues 선택 가능한 값 리스트
 * @param createdAt 생성 시각
 */
public record ClothesAttributeDefDto(
    UUID id,
    String name,
    List<String> selectableValues,
    Instant createdAt
) {

}
