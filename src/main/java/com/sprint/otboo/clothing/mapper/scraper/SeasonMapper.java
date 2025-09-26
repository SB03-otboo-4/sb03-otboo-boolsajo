package com.sprint.otboo.clothing.mapper.scraper;

import com.sprint.otboo.clothing.entity.attribute.Season;
import java.util.List;
import java.util.Locale;

/**
 * 계절 문자열 매핑 유틸리티
 *
 * <p>임의의 문자열을 {@link Season} Enum으로 변환하는 기능을 제공</p>
 *
 * <ul>
 *   <li>영문 및 한글 표기를 지원</li>
 *   <li>대소문자 및 공백 무시</li>
 *   <li>포함된 키워드 기반으로 매핑</li>
 *   <li>완전 일치 키워드 우선, 이후 부분 일치로 매핑</li>
 *   <li>매칭 불가능 시 {@code null} 반환</li>
 * </ul>
 */
public class SeasonMapper {

    /**
     * 문자열을 {@link Season} Enum으로 매핑
     *
     * @param value 계절 문자열
     * @return {@link Season} 변환된 Enum 값, 매칭되지 않으면 {@code null}
     */
    public static Season map(String value) {
        if (value == null || value.isBlank()) return null;

        String v = value.toLowerCase(Locale.ROOT).trim();

        // 1. 완전 일치 확인
        for (Season season : Season.values()) {
            for (String keyword : getKeywords(season)) {
                if (v.equalsIgnoreCase(keyword)) return season;
            }
        }

        // 2. 부분 일치 확인
        for (Season season : Season.values()) {
            for (String keyword : getKeywords(season)) {
                if (v.contains(keyword.toLowerCase(Locale.ROOT))) return season;
            }
        }

        return null;
    }

    /**
     * 계절별 매칭 키워드 목록 반환
     *
     * <p>영문 및 한글 표기를 포함</p>
     *
     * @param season {@link Season} Enum 값
     * @return 해당 계절과 매칭 가능한 문자열 목록
     */
    private static List<String> getKeywords(Season season) {
        return switch (season) {
            case SPRING -> List.of("spring", "봄");
            case SUMMER -> List.of("summer", "여름");
            case FALL -> List.of("fall", "autumn", "가을");
            case WINTER -> List.of("winter", "겨울");
        };
    }
}