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

    // 다국어 처리 : 한국어
    public String getKoreanName() {
        return switch (this) {
            case LIGHT -> "얇음";
            case MEDIUM -> "보통";
            case HEAVY -> "두꺼움";
        };
    }
}
