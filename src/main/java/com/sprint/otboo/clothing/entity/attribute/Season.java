package com.sprint.otboo.clothing.entity.attribute;

/**
 * 계절(Season)을 나타내는 열거형(enum).
 * <p>
 * SPRING - 봄 / Spring<br>
 * SUMMER - 여름 / Summer<br>
 * FALL   - 가을 / Fall<br>
 * WINTER - 겨울 / Winter
 */
public enum Season {
    SPRING,
    SUMMER,
    FALL,
    WINTER;

    /**
     * 문자열을 Season enum으로 변환
     * <p>
     * 한글/영어 모두 지원하며, 매핑되지 않으면 null 반환
     *
     * @param value 계절 문자열 (예: "SPRING", "봄")
     * @return 해당하는 Season enum 또는 null
     */
    public static Season fromString(String value) {
        if (value == null) return null;

        return switch (value.toUpperCase()) {
            case "SPRING", "봄" -> SPRING;
            case "SUMMER", "여름" -> SUMMER;
            case "FALL", "가을" -> FALL;
            case "WINTER", "겨울" -> WINTER;
            default -> null;
        };
    }
}
