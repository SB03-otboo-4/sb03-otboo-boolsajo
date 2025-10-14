package com.sprint.otboo.clothing.mapper.scraper;

import com.sprint.otboo.clothing.entity.attribute.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 색상 문자열 매핑 유틸리티
 *
 * <p>임의의 문자열을 {@link Color} Enum으로 변환하는 기능을 제공</p>
 *
 * <ul>
 *   <li>영문, 한글, 약어 등 다양한 표기 지원</li>
 *   <li>대소문자 및 공백 무시</li>
 *   <li>복합 키워드 우선 처리</li>
 *   <li>정규식 패턴 기반 매칭</li>
 *   <li>매칭 불가능 시 {@link Color#UNKNOWN} 반환</li>
 * </ul>
 */
public class ColorMapper {

    // 정규화된 입력값 캐싱
    private static final Map<String, String> normalizedCache = new HashMap<>();

    // 키워드 -> Pattern 캐싱
    private static final Map<String, Pattern> patternCache = new HashMap<>();

    /**
     * 문자열을 {@link Color} Enum으로 매핑
     *
     * @param value 색상 문자열
     * @return {@link Color} 변환된 Enum 값, 매칭되지 않으면 {@link Color#UNKNOWN}
     */
    public static Color map(String value) {
        if (value == null || value.isBlank()) return Color.UNKNOWN;

        String normalizedValue = normalize(value);

        // 1. 완전 일치 확인
        for (Color color : Color.values()) {
            if (color == Color.UNKNOWN) continue;
            for (String keyword : getKeywords(color)) {
                if (normalizedValue.equals(normalize(keyword))) {
                    return color;
                }
            }
        }

        // 2. 정규식 부분 일치 확인
        List<Color> sortedColors = Arrays.stream(Color.values())
            .filter(c -> c != Color.UNKNOWN)
            .toList();

        for (Color color : sortedColors) {
            for (String keyword : getKeywords(color)) {
                Pattern pattern = patternCache.computeIfAbsent(keyword,
                    k -> Pattern.compile(k, Pattern.CASE_INSENSITIVE));
                if (pattern.matcher(value).find()) { // 원본 값으로 매칭
                    return color;
                }
            }
        }

        return Color.UNKNOWN;
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
     * 색상별 매칭 키워드 목록 반환
     *
     * <p>영문 표기, 한글 표기, 약어, 혼용 표기 등 다양한 케이스를 포함</p>
     *
     * @param color {@link Color} Enum 값
     * @return 해당 색상과 매칭 가능한 문자열 목록
     */
    private static List<String> getKeywords(Color color) {
        return switch (color) {
            case SKYBLUE -> List.of("skyblue", "스카이블루", "연청", "연파랑", "연블루", "light[- ]blue");
            case BLUE -> List.of("blue", "블루", "파랑", "파란", "청색", "다크블루");
            case BLACK -> List.of("black", "다크", "블랙", "검정", "dark");
            case WHITE -> List.of("white", "화이트", "흰색", "순백", "진주색", "펄");
            case RED -> List.of("red", "레드", "빨강", "빨간", "적색", "와인", "wine", "burgundy", "maroon", "자주");
            case CHARCOAL -> List.of("charcoal", "차콜", "챠콜", "차콜그레이");
            case GREY -> List.of("grey", "gray", "그레이", "멜란지", "회색", "애쉬", "아쉬");
            case YELLOW -> List.of("yellow", "옐로우", "노랑", "노란", "황색", "mustard", "라임", "lime");
            case MINT -> List.of("mint", "민트", "연두", "애플그린");
            case KHAKI -> List.of("khaki", "카키", "올리브그린");
            case GREEN -> List.of("green", "그린", "초록", "녹색", "카키그린");
            case PINK -> List.of("pink", "핑크", "분홍", "코랄핑크", "로즈", "장미색");
            case BROWN -> List.of("brown", "브라운", "갈색", "밤색", "chocolate");
            case BEIGE -> List.of("beige", "베이지", "샌드", "피치", "복숭아", "peach");
            case ORANGE -> List.of("orange", "오렌지", "주황");
            case PURPLE -> List.of("purple", "퍼플", "보라", "바이올렛", "violet");
            case NAVY -> List.of("navy", "네이비", "곤색", "진청");
            case LAVENDER -> List.of("lavender", "라벤더", "light[- ]?purple");
            case OLIVE -> List.of("olive", "올리브");
            case IVORY -> List.of("ivory", "아이보리");
            case CREAM -> List.of("cream", "크림");
            case GOLD -> List.of("gold", "골드", "금색", "코퍼", "copper");
            case SILVER -> List.of("silver", "실버", "은색");
            case TRANSPARENT -> List.of("transparent", "투명", "clear");
            case MULTI -> List.of("multi", "멀티", "믹스", "컬러풀", "혼합[- ]?색상", "배색");
            default -> List.of();
        };
    }
}