package com.sprint.otboo.clothing.entity.attribute;

/**
 * 의상 색상 Enum
 *
 * <p>의상에 사용되는 다양한 색상을 정의</p>
 *
 * <ul>
 *   <li>기본 색상: BLACK, WHITE, RED, BLUE, YELLOW, GREEN 등</li>
 *   <li>중간/파생 색상: GREY, NAVY, KHAKI, SKYBLUE, MINT, LAVENDER 등</li>
 *   <li>특수 색상: GOLD, SILVER, TRANSPARENT, MULTI</li>
 *   <li>매칭 불가능 시 {@link #UNKNOWN}</li>
 * </ul>
 */
public enum Color {
    BLACK, WHITE, RED, BLUE, GREY, YELLOW, GREEN, PINK, BROWN, BEIGE,
    ORANGE, PURPLE, NAVY, KHAKI, SKYBLUE, MINT, LAVENDER, OLIVE, CHARCOAL,
    IVORY, CREAM, GOLD, SILVER, TRANSPARENT, MULTI, UNKNOWN;

    /**
     * 문자열을 기반으로 {@link Color} 매핑
     *
     * <p>
     * - 영문 및 한글 표기를 모두 지원
     * - 대소문자 및 공백을 무시하고 비교
     * - 일치하지 않으면 {@link #UNKNOWN} 반환
     * </p>
     *
     * @param value 색상 문자열
     * @return {@link Color} 변환된 Enum 값
     */
    public static Color fromString(String value) {
        if (value == null) return UNKNOWN;
        String v = value.toLowerCase().trim();

        return switch (v) {
            case "black", "블랙", "검정", "검정색" -> BLACK;
            case "white", "화이트", "흰색", "화이트색" -> WHITE;
            case "red", "레드", "빨강", "빨간", "적색" -> RED;
            case "blue", "블루", "파랑", "파란", "청색" -> BLUE;
            case "grey", "gray", "그레이", "멜란지", "회색", "애쉬", "아쉬" -> GREY;
            case "yellow", "옐로우", "노랑", "노란", "황색" -> YELLOW;
            case "green", "그린", "초록", "녹색", "카키그린" -> GREEN;
            case "pink", "핑크", "분홍" -> PINK;
            case "brown", "브라운", "갈색", "밤색" -> BROWN;
            case "beige", "베이지", "샌드" -> BEIGE;
            case "orange", "오렌지", "주황", "주황색" -> ORANGE;
            case "purple", "퍼플", "보라", "보라색", "바이올렛" -> PURPLE;
            case "navy", "네이비", "곤색", "진청" -> NAVY;
            case "khaki", "카키", "올리브그린" -> KHAKI;
            case "skyblue", "스카이블루", "연청", "연파랑" -> SKYBLUE;
            case "mint", "민트", "연두", "애플그린" -> MINT;
            case "lavender", "라벤더", "연보라" -> LAVENDER;
            case "olive", "올리브", "올리브색" -> OLIVE;
            case "charcoal", "차콜", "챠콜", "먹색" -> CHARCOAL;
            case "ivory", "아이보리", "아이보리색" -> IVORY;
            case "cream", "크림", "크림색" -> CREAM;
            case "gold", "골드", "금색" -> GOLD;
            case "silver", "실버", "은색" -> SILVER;
            case "투명", "transparent", "clear" -> TRANSPARENT;
            case "multi", "멀티", "믹스", "컬러풀" -> MULTI;
            default -> UNKNOWN;
        };
    }
}