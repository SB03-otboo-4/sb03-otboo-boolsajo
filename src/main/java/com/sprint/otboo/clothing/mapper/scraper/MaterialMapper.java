package com.sprint.otboo.clothing.mapper.scraper;

import com.sprint.otboo.clothing.entity.attribute.Material;
import java.util.Arrays;
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
 *   <li>완전 일치 키워드 우선, 이후 부분 일치로 매핑</li>
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

        String normalizedValue = normalize(value);

        // 1. 완전 일치 확인
        for (Material material : Material.values()) {
            if (material == Material.UNKNOWN) continue;
            for (String keyword : getKeywords(material)) {
                if (normalizedValue.equals(normalize(keyword))) {
                    return material;
                }
            }
        }

        // 2. 부분 일치 확인 (복합 키워드 우선)
        List<Material> sortedMaterials = Arrays.stream(Material.values())
            .filter(m -> m != Material.UNKNOWN)
            .sorted((a, b) -> {
                // _BLEND 포함 이름을 우선
                boolean aBlend = a.name().contains("_BLEND");
                boolean bBlend = b.name().contains("_BLEND");
                return Boolean.compare(bBlend, aBlend); // bBlend true 먼저
            })
            .toList();

        for (Material material : sortedMaterials) {
            for (String keyword : getKeywords(material)) {
                if (normalizedValue.contains(normalize(keyword))) {
                    return material;
                }
            }
        }

        return Material.UNKNOWN;
    }

    /**
     * 문자열 정규화
     * <ul>
     *   <li>소문자 변환</li>
     *   <li>공백 제거</li>
     * </ul>
     */
    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
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
            // 복합 키워드를 단순 키워드보다 먼저 배치
            case COTTON_BLEND -> List.of("cotton blend", "코튼혼방", "코튼 혼방", "면혼방");
            case POLYESTER_BLEND -> List.of("polyester blend", "폴리혼방", "폴리 혼방");
            case WOOL_BLEND -> List.of("wool blend", "울혼방", "울 혼방");
            // 단일 키워드
            case COTTON -> List.of("cotton", "코튼", "면");
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
            case NYLON_BLEND -> List.of("nylon blend", "나일론혼방", "나일론 혼방");
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