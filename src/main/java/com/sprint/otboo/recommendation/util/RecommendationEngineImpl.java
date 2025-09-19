package com.sprint.otboo.recommendation.util;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.Season;
import com.sprint.otboo.clothing.entity.attribute.Thickness;
import com.sprint.otboo.recommendation.entity.TemperatureCategory;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import java.util.List;
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
     * 사용자의 의상 리스트와 체감온도, 날씨 정보를 기반으로 추천 의상 리스트 반환
     *
     * @param clothes 사용자 의상 리스트
     * @param perceivedTemp 체감 온도 (°C)
     * @param weather 날씨 정보
     * @return 추천 대상 의상 리스트
     */
    @Override
    public List<Clothes> recommend(List<Clothes> clothes, double perceivedTemp, Weather weather
    ) {
        // 풍속 초기화
        this.windSpeed = (weather.getSpeedMs() != null) ? weather.getSpeedMs() : 0.0;

        // 1. 계절 판별 (봄 / 여름 / 가을 / 겨울)
        Season season = WeatherUtils.classifySeason(perceivedTemp);

        // 2. 세부 온도 범주 판별 (각 계절 내 LOW / HIGH 구간)
        TemperatureCategory category = WeatherUtils.classifyTemperatureCategory(season, perceivedTemp);

        // 3. 추천 필터링
        return clothes.stream()
            .filter(c -> matchesSeasonAndCategory(c, season, category, weather))
            .toList();
    }

    /**
     * 계절 + 세부 온도 범주 + 날씨 기반 필터링
     *
     * @param clothes 의상
     * @param season 계절
     * @param category 세부 온도 범주
     * @param weather 날씨 정보
     * @return 추천 가능 여부
     */
    private boolean matchesSeasonAndCategory(Clothes clothes, Season season, TemperatureCategory category, Weather weather
    ) {
        // 1. 의상 타입 기반 필터링
        boolean typeRule = matchesTypeRule(clothes, season, category, weather);

        // 2. 두께 기반 필터
        boolean thicknessRule = clothes.getAttributes().stream()
            .filter(attr -> attr.getDefinition() != null
                && "thickness".equalsIgnoreCase(attr.getDefinition().getName()))
            .map(attr -> {
                try {
                    return Thickness.valueOf(attr.getValue().toUpperCase());
                } catch (Exception e) {
                    return null;
                }
            })
            // Nullable : 값이 있다면 규칙 적용 / 없다면 규칙 패스
            .allMatch(thick -> thick == null || isSuitableThickness(clothes.getType(), thick, season, category));

        // 3. 일교차 기반 OUTER 추천 확장
        double dailyRange = WeatherUtils.calculateDailyRange(weather.getMaxC(), weather.getMinC());
        boolean dailyRangeRule = true;

        // 봄/가을, 일교차 6도 이상이면 OUTER 강제 추천
        if ((season == Season.SPRING || season == Season.FALL) && dailyRange >= 6) {
            dailyRangeRule = clothes.getType() == ClothesType.OUTER;
        }

        return typeRule && thicknessRule && dailyRangeRule;
    }

    /**
     * 의상 타입별 기본 필터링
     *
     * @param clothes 의상
     * @param season 계절
     * @param category 세부 온도 범주
     * @param weather 날씨 정보
     * @return 추천 가능 여부
     */
    private boolean matchesTypeRule(Clothes clothes, Season season, TemperatureCategory category, Weather weather
    ) {
        return switch (clothes.getType()) {
            case OUTER -> matchesOuterRule(season, category, weather);
            case HAT -> matchesHatRule(season, category, weather);
            case SCARF -> matchesScarfRule(season, category, weather);
            default ->
                // TOP, BOTTOM, UNDERWEAR, ACCESSORY, SHOES, SOCKS, ETC 등 기본 추천
                true;
        };

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
                || (category == TemperatureCategory.HIGH && weather.getType() == PrecipitationType.RAIN))
                && windSpeed > 5;
            case SUMMER -> (category == TemperatureCategory.LOW && weather.getSkyStatus() == SkyStatus.CLEAR);
            case FALL -> category == TemperatureCategory.HIGH && windSpeed > 5;
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
     * <ul>
     *   <li><b>OUTER</b>
     *     <ul>
     *       <li>SPRING: HIGH -> 얇거나 중간, LOW -> 중간</li>
     *       <li>SUMMER: LIGHT</li>
     *       <li>FALL: HIGH -> 얇거나 중간, LOW -> 중간 또는 두껍게</li>
     *       <li>WINTER: HEAVY</li>
     *     </ul>
     *   </li>
     *   <li><b>TOP & BOTTOM</b>
     *     <ul>
     *       <li>SPRING: HIGH -> 얇거나 중간, LOW -> 중간</li>
     *       <li>SUMMER: LIGHT</li>
     *       <li>FALL: HIGH -> 중간, LOW -> 중간 또는 두껍게</li>
     *       <li>WINTER: HEAVY</li>
     *     </ul>
     *   </li>
     *   <li>그 외 타입: 두께 제한 없음</li>
     * </ul>
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
            // OUTER 의상 두께 규칙
            case OUTER -> switch (season) {
                case SPRING -> switch (category) {
                    case HIGH -> thickness == Thickness.LIGHT || thickness == Thickness.MEDIUM;
                    case LOW  -> thickness == Thickness.MEDIUM;
                };
                case SUMMER -> thickness == Thickness.LIGHT;
                case FALL -> switch (category) {
                    case HIGH -> thickness == Thickness.LIGHT || thickness == Thickness.MEDIUM;
                    case LOW  -> thickness == Thickness.MEDIUM || thickness == Thickness.HEAVY;
                };
                case WINTER -> thickness == Thickness.HEAVY;
            };

            // TOP & BOTTOM 의상 두께 규칙
            case TOP, BOTTOM -> switch (season) {
                case SPRING -> switch (category) {
                    case HIGH -> thickness == Thickness.LIGHT || thickness == Thickness.MEDIUM;
                    case LOW  -> thickness == Thickness.MEDIUM;
                };
                case SUMMER -> thickness == Thickness.LIGHT;
                case FALL -> switch (category) {
                    case HIGH -> thickness == Thickness.MEDIUM;
                    case LOW  -> thickness == Thickness.MEDIUM || thickness == Thickness.HEAVY;
                };
                case WINTER -> thickness == Thickness.HEAVY;
            };

            // 기타 의상 타입: 두께 제한 없음
            default -> true;
        };
    }
}