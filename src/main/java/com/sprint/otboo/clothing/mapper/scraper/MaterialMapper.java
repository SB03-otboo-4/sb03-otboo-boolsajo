package com.sprint.otboo.clothing.mapper.scraper;

import com.sprint.otboo.clothing.entity.attribute.Material;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 소재 문자열 매핑 유틸리티
 *
 * <p>임의의 문자열을 {@link Material} Enum으로 변환하는 기능을 제공</p>
 *
 * <ul>
 *   <li>영문/한글 표기 지원</li>
 *   <li>대소문자 무시</li>
 *   <li>복합 키워드 우선 처리</li>
 *   <li>정규식 패턴 기반 매칭</li>
 *   <li>매칭 불가능 시 {@link Material#UNKNOWN} 반환</li>
 * </ul>
 */
public class MaterialMapper {

    // 정규화된 입력값 캐싱
    private static final Map<String, String> normalizedCache = new HashMap<>();

    // 키워드 -> Pattern 캐싱
    private static final Map<String, Pattern> patternCache = new HashMap<>();

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

        // 2. 정규식 부분 일치 확인 (복합 키워드 우선)
        List<Material> sortedMaterials = Arrays.stream(Material.values())
            .filter(m -> m != Material.UNKNOWN)
            .sorted((a, b) -> {
                boolean aBlend = a.name().contains("_BLEND");
                boolean bBlend = b.name().contains("_BLEND");
                return Boolean.compare(bBlend, aBlend);
            })
            .toList();

        for (Material material : sortedMaterials) {
            for (String keyword : getKeywords(material)) {
                Pattern pattern = patternCache.computeIfAbsent(keyword,
                    k -> Pattern.compile(k, Pattern.CASE_INSENSITIVE));
                if (pattern.matcher(value).find()) { // 원본 값으로 매칭
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
        return normalizedCache.computeIfAbsent(s, k -> k.toLowerCase(Locale.ROOT).replaceAll("\\s+", ""));
    }

    /**
     * 소재별 매칭 키워드 목록 반환
     *
     * <p>복합 키워드 우선, 정규식 패턴 적용</p>
     */
    private static List<String> getKeywords(Material material) {
        return switch (material) {
            // 복합 키워드 ( Blend )
            case COTTON_BLEND -> List.of("cotton[- +]?blend", "코튼[- +]?혼방", "면[- +]?혼방");
            case POLYESTER_BLEND -> List.of("polyester[- ]?blend", "폴리[- ]?혼방", "폴리실 혼방");
            case WOOL_BLEND -> List.of("wool[- ]?blend", "울[- ]?혼방", "울실 혼방");

            // 혼합 소재
            case COTTON_POLY -> List.of("cotton[- ]?poly", "면[- ]?폴리", "코튼[- ]?폴리");

            // 단일 키워드 ( 기본 소재 )
            case COTTON -> List.of("cotton", "코튼", "면", "\\bco\\b", "ctn");
            case POLYESTER -> List.of("polyester", "폴리에스터", "폴리", "\\bpe\\b", "poly");
            case WOOL -> List.of("wool", "\\b울\\b", "양모", "\\bwl\\b");
            case NYLON -> List.of("nylon", "나일론");
            case LINEN -> List.of("linen", "린넨", "마", "리넨");
            case ACRYLIC -> List.of("acrylic", "아크릴");
            case SPANDEX -> List.of("spandex", "스판", "스판덱스");
            case RAYON -> List.of("rayon", "레이온");
            case SILK -> List.of("silk", "실크", "실키");
            case DENIM -> List.of("denim", "데님", "\\b청\\b", "청지");
            case LEATHER -> List.of("leather", "가죽", "레더");
            case CASHMERE -> List.of("cashmere", "캐시미어", "캐시");
            case ANGORA -> List.of("angora", "앙고라");
            case LAMB_WOOL -> List.of("lambswool", "램스울", "램[- ]?울");
            case FUR -> List.of("fur", "\\b퍼\\b", "모피");
            case NYLON_BLEND -> List.of("nylon[- ]?blend", "나일론[- ]?혼방");

            // 특수 / 패턴 소재
            case RIB -> List.of("리브", "rib", "ribbed", "리브드");
            case JERSEY -> List.of("저지", "jersey", "저지[- ]?소재");
            case TERRY -> List.of("테리", "terry");
            case FLEECE -> List.of("플리스", "fleece", "양털", "furry");
            case CHIFFON -> List.of("쉬폰", "chiffon");
            case ORGANZA -> List.of("오간자", "organza");
            case SATIN -> List.of("새틴", "satin");
            case CORDUROY -> List.of("코듀로이", "corduroy");
            case ALPACA -> List.of("알파카", "alpaca");

            // 신소재
            case MODAL -> List.of("modal", "모달");
            case TENCEL -> List.of("tencel", "텐셀", "리오셀");
            case PU -> List.of("\\bpu\\b", "폴리우레탄");

            // 매핑 불가
            default -> List.of();
        };
    }
}