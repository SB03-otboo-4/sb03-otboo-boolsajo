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
}