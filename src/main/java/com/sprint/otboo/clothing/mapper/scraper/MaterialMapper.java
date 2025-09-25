package com.sprint.otboo.clothing.mapper.scraper;

import com.sprint.otboo.clothing.entity.attribute.Material;
import java.util.List;
import java.util.Locale;

/**
 * 소재 문자열 매핑 유틸리티
 *
 * <p>임의의 문자열을 {@link Material} Enum으로 변환하는 기능을 제공</p>
 *
 * <ul>
 *   <li>다양한 영문/한글 표기를 지원</li>
 *   <li>대소문자 및 공백 무시</li>
 *   <li>포함된 키워드 기반으로 매핑</li>
 *   <li>매칭 불가능 시 {@link Material#UNKNOWN} 반환</li>
 * </ul>
 */
public class MaterialMapper {

    /**
     * 문자열을 {@link Material} Enum으로 매핑
     *
     * @param value 소재 문자열
     * @return {@link Material} 변환된 Enum 값, 매칭되지 않으면 {@link Material#UNKNOWN}
     */
    public static Material map(String value) {
        if (value == null || value.isBlank()) return Material.UNKNOWN;

        String v = value.toLowerCase(Locale.ROOT).trim();

        for (Material material : Material.values()) {
            if (material == Material.UNKNOWN) continue;
            for (String keyword : getKeywords(material)) {
                if (v.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return material;
                }
            }
        }

        return Material.UNKNOWN;
    }

    /**
     * 소재별 매칭 키워드 목록 반환
     *
     * <p>영문 표기, 한글 표기, 혼용 표기 등 다양한 케이스를 포함</p>
     *
     * @param material {@link Material} Enum 값
     * @return 해당 소재와 매칭 가능한 문자열 목록
     */
    private static List<String> getKeywords(Material material) {
        return switch (material) {
            case COTTON -> List.of("cotton", "면", "코튼");
            case POLYESTER -> List.of("polyester", "폴리에스터", "폴리");
            case WOOL -> List.of("wool", "울", "양모");
            case NYLON -> List.of("nylon", "나일론");
            case LINEN -> List.of("linen", "린넨", "마");
            case ACRYLIC -> List.of("acrylic", "아크릴");
            case SPANDEX -> List.of("spandex", "스판", "스판덱스");
            case RAYON -> List.of("rayon", "레이온");
            case SILK -> List.of("silk", "실크");
            case DENIM -> List.of("denim", "데님", "청", "청지");
            case LEATHER -> List.of("leather", "가죽", "레더");
            case CASHMERE -> List.of("cashmere", "캐시미어");
            case ANGORA -> List.of("angora", "앙고라");
            case LAMB_WOOL -> List.of("lambswool", "램스울", "램 울");
            case FUR -> List.of("fur", "퍼", "모피");
            case NYLON_BLEND -> List.of("nylon blend", "나일론혼방");
            case POLY_COTTON -> List.of("poly cotton", "폴리코튼");
            case VISCOSE -> List.of("viscose", "비스코스");
            case MODAL -> List.of("modal", "모달");
            case TENCEL -> List.of("tencel", "텐셀");
            case COTTON_BLEND -> List.of("면혼방", "코튼혼방", "cotton blend");
            case WOOL_BLEND -> List.of("울혼방", "wool blend");
            case POLYESTER_BLEND -> List.of("폴리혼방", "polyester blend");
            case RIB -> List.of("리브", "rib");
            case JERSEY -> List.of("저지", "jersey");
            case TERRY -> List.of("테리", "terry");
            case FLEECE -> List.of("플리스", "fleece");
            case CHIFFON -> List.of("쉬폰", "chiffon");
            case ORGANZA -> List.of("오간자", "organza");
            case SATIN -> List.of("새틴", "satin");
            case CORDUROY -> List.of("코듀로이", "corduroy");
            case ALPACA -> List.of("알파카", "alpaca");
            default -> List.of();
        };
    }
}