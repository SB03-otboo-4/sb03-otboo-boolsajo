package com.sprint.otboo.clothing.mapper.scraper;

import com.sprint.otboo.clothing.entity.attribute.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 사이즈 문자열 매핑 유틸리티
 *
 * <p>임의의 문자열을 {@link Size} Enum으로 변환하는 기능을 제공</p>
 *
 * <ul>
 *   <li>다양한 영문/한글 표기를 지원</li>
 *   <li>대소문자 및 공백 무시</li>
 *   <li>복합 키워드 우선 처리</li>
 *   <li>정규식 패턴 기반 매칭</li>
 *   <li>매칭 불가능 시 {@link Size#UNKNOWN} 반환</li>
 * </ul>
 */
public class SizeMapper {

    // 정규화된 입력값 캐싱
    private static final Map<String, String> normalizedCache = new HashMap<>();

    // 키워드 -> Pattern 캐싱
    private static final Map<String, Pattern> patternCache = new HashMap<>();

    // 우선순위 고정 리스트
    public static final List<Size> PRIORITY_ORDER = List.of(
        Size.FREE,
        Size.ONE_SIZE,
        Size.XXXL,
        Size.XXL,
        Size.XL,
        Size.XS,
        Size.L,
        Size.M,
        Size.S
    );

    /**
     * 문자열을 {@link Size} Enum으로 매핑
     *
     * @param value 사이즈 문자열
     * @return {@link Size} 변환된 Enum 값, 매칭되지 않으면 {@link Size#UNKNOWN}
     */
    public static Size map(String value) {
        if (value == null || value.isBlank()) return Size.UNKNOWN;

        String normalizedValue = normalize(value);

        // 우선순위 순서대로만 탐색
        for (Size size : PRIORITY_ORDER) {
            for (String keyword : getKeywords(size)) {
                // 완전 일치
                if (normalizedValue.equals(normalize(keyword))) {
                    return size;
                }
                // 정규식 / 부분 일치
                Pattern pattern = patternCache.computeIfAbsent(keyword,
                    k -> Pattern.compile(k, Pattern.CASE_INSENSITIVE));
                if (pattern.matcher(value).find()) {
                    return size;
                }
            }
        }

        return Size.UNKNOWN;
    }

    /**
     * 문자열 정규화
     * <ul>
     *   <li>소문자 변환</li>
     *   <li>공백 제거</li>
     * </ul>
     */
    private static String normalize(String s) {
        return normalizedCache.computeIfAbsent(s, k -> k.toLowerCase(Locale.ROOT).replaceAll("\\s+", ""));
    }

    /**
     * 사이즈별 매칭 키워드 목록 반환
     *
     * <p>영문 표기, 한글 표기, 축약형 등 다양한 케이스를 포함</p>
     *
     * @param size {@link Size} Enum 값
     * @return 해당 사이즈와 매칭 가능한 문자열 목록
     */
    private static List<String> getKeywords(Size size) {
        return switch (size) {
            case FREE -> List.of("FREE", "프리", "FREE[- ]?SIZE", "프리[- ]?사이즈");
            case ONE_SIZE -> List.of("ONE", "ONE[- ]?SIZE", "Single[- ]?Size");
            case XXXL -> List.of("XXXL", "3XL", "3X[- ]?LARGE", "트리플[- ]?엑스라지", "쓰리[- ]?엑스", "쓰리[- ]?엑스라지");
            case XXL -> List.of("XXL",  "XX[- ]?Large", "2XL", "2X[- ]?LARGE", "더블[- ]?엑스라지", "투[- ]?엑스", "투[- ]?엑스라지");
            case XL -> List.of("XL", "X[- ]?LARGE", "Extra[- ]?Large", "빅[- ]?라지", "엑스[- ]?라지", "엑스엘");
            case XS -> List.of("XS", "X[- ]?Small", "Extra[- ]?Small", "엑스[- ]?스몰", "작은[- ]?사이즈");
            case L -> List.of("L", "Large", "Big", "라지", "빅");
            case M -> List.of("M", "Medium", "미디움", "미디엄", "중간");
            case S -> List.of("S", "Small", "스몰", "스몰[- ]?사이즈");
            default -> List.of();
        };
    }
}