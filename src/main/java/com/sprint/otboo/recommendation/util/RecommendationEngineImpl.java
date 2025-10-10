package com.sprint.otboo.recommendation.util;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.Season;
import com.sprint.otboo.clothing.entity.attribute.Thickness;
import com.sprint.otboo.recommendation.entity.TemperatureCategory;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 사용자 의상 추천 엔진 구현체
 * <p>
 * 계절, 체감온도, 세부 온도 범주, 일교차 및 의류 속성 기반으로
 * 사용자가 보유한 의상 중 적합한 의상 추천
 */
@Service
public class RecommendationEngineImpl implements RecommendationEngine {

    // 추천 기준 풍속 (m/s)
    private double windSpeed;

    /**
     * 사용자의 의상 리스트와 체감온도, 날씨 정보를 기반으로
     * 추천 의상 리스트를 반환
     *
     * <p>
     * - 계절, 세부 온도 범주, 타입, 두께 및 날씨 규칙을 적용
     * - 동일 타입 의상이 여러 개일 경우, 타입별로 1개만 선택
     *
     * @param clothes 사용자 의상 리스트
     * @param perceivedTemp 체감 온도 (°C)
     * @param weather 날씨 정보
     * @return 추천 대상 의상 리스트 (타입별 1개)
     */
    @Override
    public List<Clothes> recommend(List<Clothes> clothes, double perceivedTemp, Weather weather, boolean excludeDress
    ) {
        // 풍속 초기화
        this.windSpeed = (weather.getSpeedMs() != null) ? weather.getSpeedMs() : 0.0;

        // 1. 계절 판별 (봄 / 여름 / 가을 / 겨울)
        Season season = WeatherUtils.classifySeason(perceivedTemp);

        // 2. 세부 온도 범주 판별 (각 계절 내 LOW / HIGH 구간)
        TemperatureCategory category = WeatherUtils.classifyTemperatureCategory(season, perceivedTemp);

        // 3. 추천 필터링
        List<Clothes> filtered = clothes.stream()
            .filter(c -> !(excludeDress && c.getType() == ClothesType.DRESS))
            .filter(c -> matchesSeasonAndCategory(c, season, category, weather))
            .toList();

        // 4. 타입별 그룹화 후 첫 번째 요소 선택
        Map<ClothesType, List<Clothes>> groupedByType = filtered.stream()
            .collect(Collectors.groupingBy(Clothes::getType));

        // 5. DRESS 포함 시 TOP & BOTTOM 제외
        if (!excludeDress && groupedByType.containsKey(ClothesType.DRESS)) {
            groupedByType.remove(ClothesType.TOP);
            groupedByType.remove(ClothesType.BOTTOM);
        }

        // 6. 타입별 첫 번째 요소 선택 후 반환
        return groupedByType.values().stream()
            .map(list -> list.get(0))
            .toList();
    }

    /**
     * 계절 + 세부 온도 범주 + 날씨 기반 필터링
     *
     * <p>
     * 1) 의상 season 속성 필터 적용( nullable )
     * 2) 일교차 기반 OUTER 강제 추천
     * 3) 타입 + 두께 필터( nullable ) 적용
     *
     * @param clothes 의상
     * @param season 계절
     * @param category 세부 온도 범주
     * @param weather 날씨 정보
     * @return 추천 가능 여부
     */
    private boolean matchesSeasonAndCategory(Clothes clothes, Season season, TemperatureCategory category, Weather weather) {
        // 1. 의상 season 속성 필터
        if (!matchesClothesSeasonAttribute(clothes, season)) {
            return false; // 계절 속성 불일치면 추천 제외
        }

        // 2. 일교차 기반 OUTER 강제 추천
        if (isForcedOuterRecommendation(clothes, season, weather)) {
            return true;
        }

        // 3. 타입 + 두께 기반 규칙 적용
        return matchesTypeAndThickness(clothes, season, category, weather);
    }

    /**
     * 일교차 기반 OUTER 강제 추천 여부
     *
     * @param clothes 의상
     * @param season 계절
     * @param weather 날씨 정보
     * @return 추천 가능 여부
     */
    private boolean isForcedOuterRecommendation(Clothes clothes, Season season, Weather weather) {
        if (clothes.getType() != ClothesType.OUTER) {
            return false;
        }

        double dailyRange = WeatherUtils.calculateDailyRange(weather.getMaxC(), weather.getMinC());

        // 규칙 1: 봄/가을 & 일교차 6도 이상
        boolean rule1 = (season == Season.SPRING || season == Season.FALL)
            && dailyRange >= 6;

        // 규칙 2: 봄/가을 & 일교차 4도 이상 & 풍속 >= 3m/s & 구름 많음
        boolean rule2 = (season == Season.SPRING || season == Season.FALL)
            && dailyRange >= 4
            && windSpeed >= 3.0
            && weather.getSkyStatus() == SkyStatus.CLOUDY;

        return rule1 || rule2;
    }

    /**
     * 의상 속성 이름과 값을 기반으로 계절 속성 필터링
     *
     * <p>
     * - 속성명이 "season" 또는 "계절"이면 필터 적용
     * - value가 SPRING, SUMMER, FALL, WINTER, 또는 한글 "봄", "여름", "가을", "겨울"이면 enum 변환
     * - 변환 실패 시 필터 통과
     *
     * @param clothes 사용자 의상
     * @param currentSeason 현재 분기된 계절
     * @return 추천 가능 여부
     */
    private boolean matchesClothesSeasonAttribute(Clothes clothes, Season currentSeason) {
        // 의상 속성 중 계절 속성 탐색
        Optional<ClothesAttribute> seasonAttrOpt = clothes.getAttributes().stream()
            .filter(attr -> attr.getDefinition() != null
                && ("season".equalsIgnoreCase(attr.getDefinition().getName())
                || "계절".equals(attr.getDefinition().getName())))
            .findFirst();

        if (seasonAttrOpt.isEmpty()) {
            return true; // 속성 없으면 통과
        }

        String value = seasonAttrOpt.get().getValue();
        if (value == null || value.isBlank()) {
            return true; // 값 없으면 통과
        }

        Season clothesSeason = Season.fromString(value);
        if (clothesSeason == null) {
            return true; // enum에 매핑되지 않으면 통과
        }

        return clothesSeason == currentSeason; // 값이 존재하면 비교
    }

    /**
     * 타입 + 두께 통합 필터
     *
     * @param clothes 의상
     * @param season 계절
     * @param category 세부 온도 범주
     * @param weather 날씨 정보
     * @return 추천 가능 여부
     */
    private boolean matchesTypeAndThickness(Clothes clothes, Season season, TemperatureCategory category, Weather weather) {
        return matchesTypeRuleOnly(clothes, season, category, weather)
            && matchesThicknessRule(clothes, season, category);
    }

    /**
     * 타입 기반 필터
     *
     * @param clothes 의상
     * @param season 계절
     * @param category 세부 온도 범주
     * @param weather 날씨 정보
     * @return 추천 가능 여부
     */
    private boolean matchesTypeRuleOnly(Clothes clothes, Season season, TemperatureCategory category, Weather weather) {
        return switch (clothes.getType()) {
            case OUTER -> matchesOuterRule(season, category, weather);
            case DRESS -> matchesDressRule(season, category, weather);
            case HAT -> matchesHatRule(season, category, weather);
            case SCARF -> matchesScarfRule(season, category, weather);
            default -> true; // TOP, BOTTOM, UNDERWEAR, ACCESSORY, SHOES, SOCKS, ETC 등 기본 추천
        };
    }

    /**
     * 의상 속성 이름과 값을 기반으로 두께 속성 필터링
     *
     * <p>
     * - 속성명이 "thickness" 또는 "두께"이면 필터 적용
     * - value가 LIGHT, MEDIUM, HEAVY, 또는 한글 "얇음", "가벼움", "보통", "두꺼움", "무거움" 이면 enum 변환
     * - 변환 실패 시 규칙 적용 없이 통과
     *
     * @param clothes 사용자 의상
     * @param season 계절
     * @param category 세부 온도 범주
     * @return 두께 필터 통과 여부
     */
    private boolean matchesThicknessRule(Clothes clothes, Season season, TemperatureCategory category) {
        // 허용 속성명 목록
        List<String> validNames = List.of("thickness", "두께");

        // 의상 속성 중 thickness 관련 속성 검색 후 변환 및 규칙 적용
        return clothes.getAttributes().stream()
            .filter(attr -> attr.getDefinition() != null
                && validNames.contains(attr.getDefinition().getName()))
            .map(attr -> Thickness.fromString(attr.getValue()))
            .allMatch(thick -> thick == null || isSuitableThickness(clothes.getType(), thick, season, category));
    }

    /**
     * OUTER 전용 추천 규칙
     * <ul>
     *     <li>SPRING: LOW 추천, HIGH+비 오는 날 추천 + 풍속>5</li>
     *     <li>SUMMER: LOW+맑음일 때 추천</li>
     *     <li>FALL: HIGH 추천 + 풍속>5</li>
     *     <li>WINTER: 무조건 추천</li>
     * </ul>
     */
    private boolean matchesOuterRule(Season season, TemperatureCategory category, Weather weather) {
        return switch (season) {
            case SPRING -> ((category == TemperatureCategory.LOW)
                || (category == TemperatureCategory.HIGH && weather.getType() == PrecipitationType.RAIN));
            case SUMMER -> (category == TemperatureCategory.LOW && weather.getSkyStatus() == SkyStatus.CLEAR);
            case FALL -> category == TemperatureCategory.HIGH;
            case WINTER -> true;
        };
    }

    /**
     * HAT 전용 추천 규칙
     * <ul>
     *     <li>SPRING: HIGH+맑음</li>
     *     <li>SUMMER: 맑음</li>
     *     <li>FALL: HIGH</li>
     *     <li>WINTER: 눈 오는 날</li>
     * </ul>
     */
    private boolean matchesHatRule(Season season, TemperatureCategory category, Weather weather) {
        return switch (season) {
            case SPRING -> category == TemperatureCategory.HIGH && weather.getSkyStatus() == SkyStatus.CLEAR;
            case SUMMER -> weather.getSkyStatus() == SkyStatus.CLEAR;
            case FALL -> category == TemperatureCategory.HIGH;
            case WINTER -> weather.getType() == PrecipitationType.SNOW;
        };
    }

    /**
     * DRESS 전용 추천 규칙
     * <ul>
     *     <li>SPRING: 비 + 풍속 ≥ 3m/s 제외, 풍속 ≥ 5m/s 제외</li>
     *     <li>SUMMER: 비 + 풍속 ≥ 3m/s 제외</li>
     *     <li>FALL: 비 + 풍속 ≥ 3m/s 제외, 풍속 ≥ 5m/s 제외, 눈 제외</li>
     *     <li>WINTER: 눈 제외</li>
     * </ul>
     */
    private boolean matchesDressRule(Season season, TemperatureCategory category, Weather weather) {
        // 지역 변수로 조건 정의
        boolean rainWithWind3 = weather.getType() == PrecipitationType.RAIN && windSpeed >= 3.0;
        boolean windOver5 = windSpeed >= 5.0;
        boolean snow = weather.getType() == PrecipitationType.SNOW;

        return switch (season) {
            case SPRING -> !rainWithWind3 && !windOver5;
            case SUMMER -> !rainWithWind3;
            case FALL -> !rainWithWind3 && !windOver5 && !snow;
            case WINTER -> !snow;
        };
    }

    /**
     * SCARF 전용 추천 규칙
     * <ul>
     *     <li>SPRING, SUMMER: 제외</li>
     *     <li>FALL: LOW 구간 또는 풍속>5</li>
     *     <li>WINTER: 무조건 추천</li>
     * </ul>
     */
    private boolean matchesScarfRule(Season season, TemperatureCategory category, Weather weather) {
        return switch (season) {
            case SPRING, SUMMER -> false;
            case FALL -> category == TemperatureCategory.LOW || windSpeed > 5;
            case WINTER -> true;
        };
    }

    /**
     * 두께 규칙 정의
     *
     * <p>
     * 의상 타입과 계절, 세부 온도 범주 기준으로 의상 두께가 적합한지 판별
     *
     * @param type 의상 타입
     * @param thickness 의상 두께
     * @param season 계절
     * @param category 세부 온도 범주
     * @return 해당 의상이 계절과 두께 규칙에 적합한지 여부
     */
    private boolean isSuitableThickness(ClothesType type, Thickness thickness, Season season, TemperatureCategory category
    ) {
        // thickness 값이 없으면 규칙 적용하지 않고 통과
        if (thickness == null) return true;

        return switch (type) {
            case OUTER -> isOuterSuitable(thickness, season, category);
            case DRESS -> isDressSuitable(thickness, season, category);
            case TOP, BOTTOM -> isTopBottomSuitable(thickness, season, category);
            default -> true; // 나머지 타입은 두께 제한 없음
        };
    }

    /**
     * OUTER 전용 두께 추천 규칙
     *   SPRING: HIGH -> 얇음 또는 중간, LOW -> 중간
     *   SUMMER: 얇음
     *   FALL: HIGH -> 얇음 또는 중간, LOW -> 중간 또는 두꺼움
     *   WINTER: 두꺼움
     */
    private boolean isOuterSuitable(Thickness thickness, Season season, TemperatureCategory category) {
        return switch (season) {
            case SPRING -> category == TemperatureCategory.HIGH
                ? thickness == Thickness.LIGHT || thickness == Thickness.MEDIUM
                : thickness == Thickness.MEDIUM;
            case SUMMER -> thickness == Thickness.LIGHT;
            case FALL -> category == TemperatureCategory.HIGH
                ? thickness == Thickness.LIGHT || thickness == Thickness.MEDIUM
                : thickness == Thickness.MEDIUM || thickness == Thickness.HEAVY;
            case WINTER -> thickness == Thickness.HEAVY;
        };
    }

    /**
     * DRESS 전용 두께 추천 규칙
     *   SPRING: HIGH -> 얇음 또는 중간, LOW -> 중간
     *   SUMMER: 얇음
     *   FALL: HIGH -> 중간, LOW -> 중간 또는 두꺼움
     *   WINTER: 두꺼움
     */
    private boolean isDressSuitable(Thickness thickness, Season season, TemperatureCategory category) {
        return switch (season) {
            case SPRING -> category == TemperatureCategory.HIGH
                ? thickness == Thickness.LIGHT || thickness == Thickness.MEDIUM
                : thickness == Thickness.MEDIUM;
            case SUMMER -> thickness == Thickness.LIGHT;
            case FALL -> category == TemperatureCategory.HIGH
                ? thickness == Thickness.MEDIUM
                : thickness == Thickness.MEDIUM || thickness == Thickness.HEAVY;
            case WINTER -> thickness == Thickness.HEAVY;
        };
    }

    /**
     * TOP & BOTTOM 전용 두께 추천 규칙
     *   SPRING: HIGH -> 얇음 또는 중간, LOW -> 중간
     *   SUMMER: 얇음
     *   FALL: HIGH -> 중간, LOW -> 중간 또는 두꺼움
     *   WINTER: 두꺼움
     */
    private boolean isTopBottomSuitable(Thickness thickness, Season season, TemperatureCategory category) {
        return switch (season) {
            case SPRING -> category == TemperatureCategory.HIGH
                ? thickness == Thickness.LIGHT || thickness == Thickness.MEDIUM
                : thickness == Thickness.MEDIUM;
            case SUMMER -> thickness == Thickness.LIGHT;
            case FALL -> category == TemperatureCategory.HIGH
                ? thickness == Thickness.MEDIUM
                : thickness == Thickness.MEDIUM || thickness == Thickness.HEAVY;
            case WINTER -> thickness == Thickness.HEAVY;
        };
    }
}