package com.sprint.otboo.clothing.entity.attribute;

/**
 * 의상 소재 Enum
 *
 * <p>의류에 사용되는 다양한 소재를 정의</p>
 *
 * <ul>
 *   <li>기본 소재: COTTON, POLYESTER, WOOL, NYLON, LINEN 등</li>
 *   <li>혼방 소재: NYLON_BLEND, POLY_COTTON, COTTON_BLEND, WOOL_BLEND, POLYESTER_BLEND</li>
 *   <li>특수 소재 및 직조 방식: RIB, JERSEY, TERRY, FLEECE, CHIFFON, ORGANZA, SATIN, CORDUROY</li>
 *   <li>고급 소재: CASHMERE, ANGORA, LAMB_WOOL, FUR, ALPACA, SILK</li>
 *   <li>매칭되지 않을 경우 {@link #UNKNOWN}</li>
 * </ul>
 */
public enum Material {
    COTTON, POLYESTER, WOOL, NYLON, LINEN, ACRYLIC, SPANDEX, RAYON,
    SILK, DENIM, LEATHER, CASHMERE, ANGORA, LAMB_WOOL, FUR,
    NYLON_BLEND, POLY_COTTON, VISCOSE, MODAL, TENCEL, COTTON_BLEND, WOOL_BLEND, POLYESTER_BLEND,
    RIB, JERSEY, TERRY, FLEECE, CHIFFON, ORGANZA, SATIN, CORDUROY, ALPACA,
    UNKNOWN;

    /**
     * 문자열을 기반으로 {@link Material} 매핑
     *
     * <p>
     * - 영문 및 한글 표기를 모두 지원
     * - 대소문자 및 공백을 무시하고 비교
     * - 혼방/특수 소재 표기도 지원
     * - 일치하지 않으면 {@link #UNKNOWN} 반환
     * </p>
     *
     * @param value 소재 문자열
     * @return {@link Material} 변환된 Enum 값
     */
    public static Material fromString(String value) {
        if (value == null) return UNKNOWN;
        String v = value.toLowerCase().trim();

        return switch (v) {
            case "cotton", "면", "코튼" -> COTTON;
            case "polyester", "폴리에스터", "폴리" -> POLYESTER;
            case "wool", "울", "양모" -> WOOL;
            case "nylon", "나일론" -> NYLON;
            case "linen", "린넨", "마" -> LINEN;
            case "acrylic", "아크릴" -> ACRYLIC;
            case "spandex", "스판", "스판덱스" -> SPANDEX;
            case "rayon", "레이온" -> RAYON;
            case "silk", "실크" -> SILK;
            case "denim", "데님", "청", "청지" -> DENIM;
            case "leather", "가죽", "레더" -> LEATHER;
            case "cashmere", "캐시미어" -> CASHMERE;
            case "angora", "앙고라" -> ANGORA;
            case "lambswool", "램스울", "램 울" -> LAMB_WOOL;
            case "fur", "퍼", "모피" -> FUR;
            case "나일론혼방", "nylon blend" -> NYLON_BLEND;
            case "poly cotton", "폴리코튼" -> POLY_COTTON;
            case "viscose", "비스코스" -> VISCOSE;
            case "modal", "모달" -> MODAL;
            case "tencel", "텐셀" -> TENCEL;
            case "cotton blend", "면혼방", "코튼혼방" -> COTTON_BLEND;
            case "wool blend", "울혼방" -> WOOL_BLEND;
            case "polyester blend", "폴리혼방" -> POLYESTER_BLEND;
            case "rib", "리브" -> RIB;
            case "jersey", "저지" -> JERSEY;
            case "terry", "테리" -> TERRY;
            case "fleece", "플리스" -> FLEECE;
            case "chiffon", "쉬폰" -> CHIFFON;
            case "organza", "오간자" -> ORGANZA;
            case "satin", "새틴" -> SATIN;
            case "corduroy", "코듀로이" -> CORDUROY;
            case "alpaca", "알파카" -> ALPACA;

            default -> UNKNOWN;
        };
    }
}