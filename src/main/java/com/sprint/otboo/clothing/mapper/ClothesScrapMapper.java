package com.sprint.otboo.clothing.mapper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.entity.attribute.Season;
import com.sprint.otboo.clothing.entity.attribute.Thickness;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.jsoup.select.Elements;

/**
 * 의류 속성(색상, 사이즈, 소재, 계절, 두께 등)을
 * 사이트별 추출값에서 표준화하여 ClothesAttributeDto로 변환하는 매퍼
 *
 * <p>
 * - 색상, 사이즈, 소재, 계절, 두께 등을 통일된 값으로 변환
 * - Season, Thickness enum 활용
 * - 여러 사이트 공용으로 사용할 수 있음
 * </p>
 */
public class ClothesScrapMapper {

    // ===== 색상 =====
    public static String normalizeColor(String color) {
        return switch (color.toLowerCase()) {
            case "black", "블랙", "검정" -> "BLACK";
            case "white", "화이트", "흰색" -> "WHITE";
            case "red", "레드", "빨강" -> "RED";
            case "blue", "블루", "파랑" -> "BLUE";
            case "grey", "gray", "그레이", "멜란지", "회색" -> "GREY";
            case "yellow", "옐로우", "노랑" -> "YELLOW";
            case "green", "그린", "초록" -> "GREEN";
            case "pink", "핑크", "분홍" -> "PINK";
            case "brown", "브라운", "갈색" -> "BROWN";
            case "beige", "베이지", "연베이지" -> "BEIGE";
            case "navy", "네이비" -> "NAVY";
            case "purple", "퍼플", "보라" -> "PURPLE";
            case "orange", "오렌지" -> "ORANGE";
            case "mint", "민트" -> "MINT";
            case "ivory", "아이보리" -> "IVORY";
            case "khaki", "카키" -> "KHAKI";
            case "skyblue", "스카이블루" -> "SKYBLUE";
            default -> color.toUpperCase();
        };
    }

    // ===== 사이즈 =====
    public static String normalizeSize(String size) {
        return switch (size.toUpperCase()) {
            case "XS", "S", "M", "L", "XL", "XXL", "XXXL" -> size.toUpperCase();
            case "FREE", "원사이즈", "프리", "ONE SIZE" -> "FREE";
            case "90", "95", "100", "105", "110" -> size.toUpperCase(); // 국내 치수
            case "28", "30", "32", "34", "36" -> size.toUpperCase(); // 바지 치수
            default -> size.toUpperCase();
        };
    }

    // ===== 소재 =====
    public static String normalizeMaterial(String material) {
        return switch (material.toLowerCase()) {
            case "cotton", "면" -> "COTTON";
            case "polyester", "폴리에스터" -> "POLYESTER";
            case "wool", "울" -> "WOOL";
            case "nylon", "나일론" -> "NYLON";
            case "linen", "린넨" -> "LINEN";
            case "acrylic", "아크릴" -> "ACRYLIC";
            case "spandex", "스판" -> "SPANDEX";
            case "rayon", "레이온" -> "RAYON";
            case "silk", "실크" -> "SILK";
            case "leather", "가죽" -> "LEATHER";
            case "polyurethane", "폴리우레탄" -> "POLYURETHANE";
            case "denim", "데님" -> "DENIM";
            case "cashmere", "캐시미어" -> "CASHMERE";
            default -> material.toUpperCase();
        };
    }

    // ===== 계절 =====
    public static String normalizeSeason(String seasonStr) {
        Season season = Season.fromString(seasonStr);
        return season != null ? season.name() : seasonStr.toUpperCase();
    }

    // ===== 두께 =====
    public static String normalizeThickness(String thicknessStr) {
        Thickness thickness = Thickness.fromString(thicknessStr);
        return thickness != null ? thickness.name() : thicknessStr.toUpperCase();
    }

    /**
     * Elements를 받아 분리/정규화 후 ClothesAttributeDto 리스트로 변환
     *
     * @param elements Jsoup Elements
     * @param normalizer 속성별 정규화 함수
     * @return ClothesAttributeDto 리스트
     */
    public static List<ClothesAttributeDto> mapAttributes(Elements elements, Function<String, String> normalizer) {
        return elements.stream()
            .flatMap(el -> Arrays.stream(el.text().split("[,/ ]"))) // 쉼표, 슬래시, 공백으로 분리
            .filter(text -> !text.isBlank())
            .map(text -> new ClothesAttributeDto(UUID.randomUUID(), normalizer.apply(text)))
            .toList();
    }

    /**
     * 계절 Elements를 Season enum 기반으로 매핑
     */
    public static List<ClothesAttributeDto> mapSeasonAttributes(Elements elements) {
        return mapAttributes(elements, ClothesScrapMapper::normalizeSeason);
    }

    /**
     * 두께 Elements를 Thickness enum 기반으로 매핑
     */
    public static List<ClothesAttributeDto> mapThicknessAttributes(Elements elements) {
        return mapAttributes(elements, ClothesScrapMapper::normalizeThickness);
    }
}