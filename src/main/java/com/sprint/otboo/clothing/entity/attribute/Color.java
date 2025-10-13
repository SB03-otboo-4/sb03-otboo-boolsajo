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
}