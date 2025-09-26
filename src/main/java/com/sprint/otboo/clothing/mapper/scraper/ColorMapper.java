package com.sprint.otboo.clothing.mapper.scraper;

import com.sprint.otboo.clothing.entity.attribute.Color;
import java.util.List;
import java.util.Locale;

/**
 * 색상 문자열 매핑 유틸리티
 *
 * <p>임의의 문자열을 {@link Color} Enum으로 변환하는 기능을 제공</p>
 *
 * <ul>
 *   <li>영문, 한글, 약어 등 다양한 표기를 지원</li>
 *   <li>대소문자 및 공백 무시</li>
 *   <li>포함된 키워드 기반 매핑</li>
 *   <li>완전 일치 키워드 우선, 이후 부분 일치로 매핑</li>
 *   <li>매칭 불가능 시 {@link Color#UNKNOWN} 반환</li>
 * </ul>
 */
public class ColorMapper {

    /**
     * 문자열을 {@link Color} Enum으로 매핑
     *
     * @param value 색상 문자열
     * @return {@link Color} 변환된 Enum 값, 매칭되지 않으면 {@link Color#UNKNOWN}
     */
    public static Color map(String value) {
        if (value == null || value.isBlank()) return Color.UNKNOWN;

        String v = value.toLowerCase(Locale.ROOT).trim();

        // 1. 완전 일치 확인
        for (Color color : Color.values()) {
            if (color == Color.UNKNOWN) continue;
            for (String keyword : getKeywords(color)) {
                if (v.equalsIgnoreCase(keyword)) {
                    return color;
                }
            }
        }

        // 2. 부분 일치 확인
        for (Color color : Color.values()) {
            if (color == Color.UNKNOWN) continue;
            for (String keyword : getKeywords(color)) {
                if (v.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return color;
                }
            }
        }

        return Color.UNKNOWN;
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
            case BLACK -> List.of("black", "블랙", "검정", "검정색", "bk", "블랙색상");
            case WHITE -> List.of("white", "화이트", "흰색", "화이트색", "wt");
            case RED -> List.of("red", "레드", "빨강", "빨간", "적색", "와인", "wine", "rd");
            case BLUE -> List.of("blue", "블루", "파랑", "파란", "청색", "blu", "진청");
            case GREY -> List.of("grey", "gray", "그레이", "멜란지", "회색", "애쉬", "아쉬", "그레이색");
            case YELLOW -> List.of("yellow", "옐로우", "노랑", "노란", "황색", "yl");
            case GREEN -> List.of("green", "그린", "초록", "녹색", "카키그린", "grn");
            case PINK -> List.of("pink", "핑크", "분홍", "pk");
            case BROWN -> List.of("brown", "브라운", "갈색", "밤색", "brn");
            case BEIGE -> List.of("beige", "베이지", "샌드", "bg");
            case ORANGE -> List.of("orange", "오렌지", "주황", "주황색", "or");
            case PURPLE -> List.of("purple", "퍼플", "보라", "보라색", "바이올렛", "pr");
            case NAVY -> List.of("navy", "네이비", "곤색", "진청", "nv");
            case KHAKI -> List.of("khaki", "카키", "올리브그린", "kh");
            case SKYBLUE -> List.of("skyblue", "스카이블루", "연청", "연파랑", "sky");
            case MINT -> List.of("mint", "민트", "연두", "애플그린", "mt");
            case LAVENDER -> List.of("lavender", "라벤더", "연보라", "lav");
            case OLIVE -> List.of("olive", "올리브", "올리브색", "ol");
            case CHARCOAL -> List.of("charcoal", "차콜", "챠콜", "먹색", "ch");
            case IVORY -> List.of("ivory", "아이보리", "아이보리색", "iv");
            case CREAM -> List.of("cream", "크림", "크림색", "cr");
            case GOLD -> List.of("gold", "골드", "금색", "gd");
            case SILVER -> List.of("silver", "실버", "은색", "sl");
            case TRANSPARENT -> List.of("transparent", "투명", "clear", "clr");
            case MULTI -> List.of("multi", "멀티", "믹스", "컬러풀", "혼합색상", "배색");
            default -> List.of();
        };
    }
}