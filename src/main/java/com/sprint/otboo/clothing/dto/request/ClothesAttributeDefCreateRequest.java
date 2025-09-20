package com.sprint.otboo.clothing.dto.request;

import java.util.List;

/**
 * 의상 속성 정의 생성 요청 DTO
 *
 * <p>클라이언트로부터 의상 속성 정의를 생성할 때 전달되는 요청 데이터입니다.
 *
 * @param name 속성 이름 (필수)
 * @param selectableValues 선택 가능한 값 리스트
 */
public record ClothesAttributeDefCreateRequest(
    String name,
    List<String> selectableValues
) {

}
