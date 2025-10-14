package com.sprint.otboo.clothing.entity.attribute;

import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 의상 속성 타입 Enum
 *
 * <p>의상에 부여되는 주요 속성의 종류를 정의</p>
 *
 * <ul>
 *   <li>{@link #COLOR} - 색상</li>
 *   <li>{@link #SIZE} - 사이즈</li>
 *   <li>{@link #MATERIAL} - 소재</li>
 *   <li>{@link #SEASON} - 계절</li>
 *   <li>{@link #THICKNESS} - 두께</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum AttributeType {
    COLOR("색상"),
    SIZE("사이즈"),
    MATERIAL("소재"),
    SEASON("계절"),
    THICKNESS("두께");

    private final String displayName;

    /**
     * 문자열을 기반으로 {@link AttributeType} 매핑
     *
     * <p>
     * - 대소문자를 구분하지 않음
     * - 일치하지 않는 값은 {@link ClothesExtractionException} 발생
     * </p>
     *
     * @param type 속성 타입 문자열
     * @return {@link AttributeType} 변환된 Enum 값
     * @throws ClothesExtractionException 알 수 없는 속성 타입일 경우 발생
     */
    public static AttributeType from(String type) {
        return switch (type.toUpperCase()) {
            case "COLOR" -> COLOR;
            case "SIZE" -> SIZE;
            case "MATERIAL" -> MATERIAL;
            case "SEASON" -> SEASON;
            case "THICKNESS" -> THICKNESS;
            default -> throw new ClothesExtractionException("알 수 없는 속성 타입: " + type);
        };
    }
}