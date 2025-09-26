package com.sprint.otboo.clothing.entity.attribute;

/**
 * 의상 사이즈 Enum
 *
 * <p>의상에 사용되는 사이즈를 표준화하여 정의</p>
 *
 * <ul>
 *   <li>국제 표준 사이즈 (XS ~ XXXL)</li>
 *   <li>자유 사이즈 (FREE, ONE_SIZE)</li>
 *   <li>매칭 불가능 시 {@link #UNKNOWN}</li>
 * </ul>
 */
public enum Size {
    XS, S, M, L, XL, XXL, XXXL, FREE, ONE_SIZE, UNKNOWN;
}