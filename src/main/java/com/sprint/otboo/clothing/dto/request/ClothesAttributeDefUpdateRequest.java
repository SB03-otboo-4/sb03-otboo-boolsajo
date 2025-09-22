package com.sprint.otboo.clothing.dto.request;

import java.util.List;

/**
 * 의상 속성 정의 수정 요청 DTO
 *
 * <p>클라이언트로부터 기존 의상 속성 정의를 수정할 때 전달되는 요청 데이터
 *
 * @param name 수정할 속성 이름 (필수)
 * @param selectableValues 수정할 선택값 리스트 (최소 1개 이상 필수)
 */
public record ClothesAttributeDefUpdateRequest(
    String name,
    List<String> selectableValues
) {

}
