package com.sprint.otboo.clothing.mapper.scraper;

import com.sprint.otboo.clothing.entity.attribute.Thickness;
import java.util.List;
import java.util.Locale;

/**
 * 두께 문자열 매핑 유틸리티
 *
 * <p>임의의 문자열을 {@link Thickness} Enum으로 변환하는 기능을 제공</p>
 *
 * <ul>
 *   <li>영문, 한글 표기를 모두 지원</li>
 *   <li>대소문자 및 공백 무시</li>
 *   <li>포함된 키워드 기반으로 매핑</li>
 *   <li>완전 일치 키워드 우선, 이후 부분 일치로 매핑</li>
 *   <li>매칭 불가능 시 {@code null} 반환</li>
 * </ul>
 */
public class ThicknessMapper {

    /**
     * 문자열을 {@link Thickness} Enum으로 매핑
     *
     * @param value 두께 문자열
     * @return {@link Thickness} 변환된 Enum 값, 매칭되지 않으면 {@code null}
     */
    public static Thickness map(String value) {
        if (value == null || value.isBlank()) return null;

        String v = value.toLowerCase(Locale.ROOT).trim();

        // 1. 완전 일치 확인
        for (Thickness thickness : Thickness.values()) {
            for (String keyword : getKeywords(thickness)) {
                if (v.equalsIgnoreCase(keyword)) return thickness;
            }
        }

        // 2. 부분 일치 확인
        for (Thickness thickness : Thickness.values()) {
            for (String keyword : getKeywords(thickness)) {
                if (v.contains(keyword.toLowerCase(Locale.ROOT))) return thickness;
            }
        }

        return null;
    }

    /**
     * 두께별 매칭 키워드 목록 반환
     *
     * <p>영문 및 한글 표기, 의미를 담은 키워드를 포함</p>
     *
     * @param thickness {@link Thickness} Enum 값
     * @return 해당 두께와 매칭 가능한 문자열 목록
     */
    private static List<String> getKeywords(Thickness thickness) {
        return switch (thickness) {
            case LIGHT -> List.of("light", "얇음", "가벼움");
            case MEDIUM -> List.of("medium", "보통");
            case HEAVY -> List.of("heavy", "두꺼움", "무거움");
        };
    }
}