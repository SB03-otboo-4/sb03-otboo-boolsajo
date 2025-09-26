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

    /**
     * 문자열을 기반으로 {@link Size} 매핑
     *
     * <p>
     * - 영문 및 한글 표기를 모두 지원
     * - 공백 및 대소문자를 무시하고 비교
     * - 일치하지 않으면 {@link #UNKNOWN} 반환
     * </p>
     *
     * @param value 사이즈 문자열
     * @return {@link Size} 변환된 Enum 값
     */
    public static Size fromString(String value) {
        if (value == null) return UNKNOWN;
        String v = value.toUpperCase().trim();

        return switch (v) {
            case "XS", "X-SMALL", "EXTRA SMALL", "엑스스몰", "엑스 스몰", "작은사이즈" -> XS;
            case "SMALL", "스몰", "스몰사이즈" -> S;
            case "MEDIUM", "미디움", "미디엄", "중간" -> M;
            case "LARGE", "BIG", "라지", "빅" -> L;
            case "XL", "X-LARGE", "EXTRA LARGE", "빅라지", "엑스라지", "엑스엘" -> XL;
            case "XXL", "XX-LARGE", "2XL", "2X-LARGE", "더블엑스라지", "투엑스", "투엑스라지" -> XXL;
            case "XXXL", "3XL", "3X-LARGE", "트리플엑스라지" -> XXXL;
            case "FREE", "원사이즈", "프리", "FREE SIZE" -> FREE;
            case "ONE", "ONE-SIZE", "ONE SIZE" -> ONE_SIZE;
            default -> UNKNOWN;
        };
    }
}