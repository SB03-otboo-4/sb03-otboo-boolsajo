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

    // 다국어 처리 : 한국어
    public String getKoreanName() {
        return switch (this) {
            case SPRING -> "봄";
            case SUMMER -> "여름";
            case FALL   -> "가을";
            case WINTER -> "겨울";
        };
    }
}
