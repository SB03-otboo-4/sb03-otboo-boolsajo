package com.sprint.otboo.clothing.entity;

/**
 * 의상 종류(ClothesType) Enum
 *
 * <p>의상 카테고리를 정의하며, 다국어 처리 가능</p>
 *
 * <ul>
 *   <li>TOP: 상의</li>
 *   <li>BOTTOM: 하의</li>
 *   <li>DRESS: 원피스</li>
 *   <li>OUTER: 아우터</li>
 *   <li>UNDERWEAR: 속옷</li>
 *   <li>ACCESSORY: 악세서리</li>
 *   <li>SHOES: 신발</li>
 *   <li>SOCKS: 양말</li>
 *   <li>HAT: 모자</li>
 *   <li>BAG: 가방</li>
 *   <li>SCARF: 스카프</li>
 *   <li>ETC: 기타</li>
 * </ul>
 */
public enum ClothesType {
    TOP,
    BOTTOM,
    DRESS,
    OUTER,
    UNDERWEAR,
    ACCESSORY,
    SHOES,
    SOCKS,
    HAT,
    BAG,
    SCARF,
    ETC;

    /**
     * 한글 이름 반환
     *
     * @return 의상 타입 한글명
     */
    public String getKoreanName() {
        return switch (this) {
            case TOP -> "상의";
            case BOTTOM -> "하의";
            case DRESS -> "원피스";
            case OUTER -> "아우터";
            case UNDERWEAR -> "속옷";
            case ACCESSORY -> "악세서리";
            case SHOES -> "신발";
            case SOCKS -> "양말";
            case HAT -> "모자";
            case BAG -> "가방";
            case SCARF -> "스카프";
            case ETC -> "기타";
        };
    }

    /**
     * 영어 이름 반환
     *
     * @return 의상 타입 영어명
     */
    public String getEnglishName() {
        return this.name();
    }
}