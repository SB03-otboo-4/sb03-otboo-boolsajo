package com.sprint.otboo.clothing.entity.attribute;

/**
 * 의류 두께를 나타내는 열거형(enum).
 * <p>
 * LIGHT  - 얇음<br>
 * MEDIUM - 보통<br>
 * HEAVY  - 두꺼움
 */
public enum Thickness {
    LIGHT,  // 얇음
    MEDIUM,  // 보통
    HEAVY;  // 두꺼움

    /**
     * 문자열을 Thickness enum으로 변환
     * <p>
     * 한글/영어 모두 지원하며, 매핑되지 않으면 null 반환
     *
     * @param value 두께 문자열 (예: "LIGHT", "보통")
     * @return 해당하는 Thickness enum 또는 null
     */
    public static Thickness fromString(String value) {
        if (value == null) return null;

        return switch (value.toUpperCase()) {
            case "LIGHT", "얇음", "가벼움" -> LIGHT;
            case "MEDIUM", "보통" -> MEDIUM;
            case "HEAVY", "두꺼움", "무거움" -> HEAVY;
            default -> null;
        };
    }
}