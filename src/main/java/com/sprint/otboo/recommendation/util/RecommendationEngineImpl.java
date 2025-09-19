package com.sprint.otboo.recommendation.util;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.Season;
import com.sprint.otboo.clothing.entity.attribute.Thickness;
import com.sprint.otboo.recommendation.entity.TemperatureCategory;
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

    /**
     * 사용자의 의상 리스트와 체감온도, 날씨 정보를 기반으로 추천 의상 리스트 반환
     *
     * @param clothes 사용자 의상 리스트
     * @param perceivedTemp 체감 온도 (°C)
     * @param weather 날씨 정보
     * @return 추천 대상 의상 리스트
     */
    @Override
    public List<Clothes> recommend(List<Clothes> clothes, double perceivedTemp, Weather weather) {
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
    private boolean matchesSeasonAndCategory(Clothes clothes, Season season,
        TemperatureCategory category, Weather weather
    ) {
        // 의상 타입 기반 기본 규칙
        boolean typeRule = switch (clothes.getType()) {
            case TOP -> switch (season) {
                case SPRING ->
                    // 봄 추천 조건:
                    // - LOW(조금 쌀쌀) 구간: 체온 보존 필요 → 추천
                    // - HIGH(따뜻) 구간: 평균온도 15°C 이상이면 얇은 옷 가능 → 추천
                    category == TemperatureCategory.HIGH || weather.getCurrentC() >= 15;

                case SUMMER ->
                    // 여름 추천 조건:
                    // - HIGH(덥게 느낌) 구간: 가벼운 상의 필요 → 추천
                    category == TemperatureCategory.HIGH;

                case FALL ->
                    // 가을 추천 조건:
                    // - LOW(서늘) 구간: 얇은 상의는 제외
                    // - HIGH(쾌적) 구간: 상의 추천 가능
                    category != TemperatureCategory.LOW;

                case WINTER ->
                    // 겨울 추천 조건:
                    // - HIGH(덜 춥게 느낌) 구간: 상의 두꺼움 확인 → 추천
                    category == TemperatureCategory.HIGH;
            };
            case BOTTOM ->
                // 추천 조건:
                // - 여름: 긴 하의 제외
                !(season == Season.SUMMER);

            case OUTER ->
                // 추천 조건:
                // - 풍속 > 5 m/s: 체감 추위 증가 → 겉옷 추천
                weather.getSpeedMs() > 5;

            default ->
                // UNDERWEAR, ACCESSORY 등 기본 통과
                true;
        };

        // 두께 기반 필터
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

        // 일교차 기반 겉옷 추천 확장
        double dailyRange = WeatherUtils.calculateDailyRange(weather.getMaxC(), weather.getMinC());
        boolean dailyRangeRule = true;
        if (dailyRange >= 6) {
            // 6도 이상이면 OUTER 추천
            dailyRangeRule = clothes.getType() == ClothesType.OUTER;
        }

        // -----------------------------
        // 추가 속성 기반 필터 확장 포인트
        // -----------------------------
        // 예: 습도, 구름 상태, 강수 여부 등
        // boolean additionalRule = clothes.getAttributes().stream()
        //         .filter(attr -> attr.getDefinition() != null)
        //         .anyMatch(attr -> /* 특정 조건 */);

        // 최종 추천 조건
        return typeRule && thicknessRule && dailyRangeRule;
    }

    /**
     * 두께 규칙 정의
     * <p>
     * 계절 및 세부 온도 범주 기준으로 의상 타입별 적합한 두께 여부 판별
     *
     * @param type 의상 타입
     * @param thickness 의상 두께
     * @param season 계절
     * @param category 세부 온도 범주
     * @return 해당 의상이 계절/온도 범주에 적합한지 여부
     */
    private boolean isSuitableThickness(ClothesType type, Thickness thickness,
        Season season, TemperatureCategory category
    ) {
        return switch (type) {
            case OUTER -> switch (season) {
                case WINTER ->
                    // 겨울 OUTER 추천 규칙
                    // - LOW(춥게 느낌): HEAVY → 추천
                    // - HIGH(덜 춥게 느낌): HEAVY 제외 → 추천 안됨
                    (category == TemperatureCategory.LOW && thickness == Thickness.HEAVY)
                    || (category == TemperatureCategory.HIGH && thickness != Thickness.HEAVY);

                case FALL, SPRING ->
                    // 봄/가을 OUTER 추천 규칙
                    // - LOW(조금 쌀쌀): MEDIUM → 추천
                    // - HIGH(따뜻): HEAVY 제외 → 추천
                    (category == TemperatureCategory.LOW && thickness == Thickness.MEDIUM)
                    || (category == TemperatureCategory.HIGH && thickness != Thickness.HEAVY);

                case SUMMER ->
                    // 여름 OUTER 추천 규칙
                    // - 모든 HIGH/LOW: LIGHT만 추천
                    thickness == Thickness.LIGHT;

                // -----------------------------
                // 추가 분기 확장 예시
                // -----------------------------
                // case SPECIAL_SEASON -> switch(category) {
                //     case LOW -> thickness == Thickness.HEAVY;
                //     case HIGH -> thickness != Thickness.HEAVY;
                // };
            };

            case TOP -> switch (season) {
                case SUMMER ->
                    // 여름 TOP 추천 규칙: LIGHT만 추천
                    thickness == Thickness.LIGHT;

                case SPRING, FALL ->
                    // 봄/가을 TOP 추천 규칙
                    // - HIGH(따뜻) 구간: LIGHT → 추천
                    // - LOW(조금 쌀쌀) 구간: LIGHT 제외 → 추천 안됨
                    (category == TemperatureCategory.HIGH && thickness == Thickness.LIGHT)
                    || (category == TemperatureCategory.LOW && thickness != Thickness.LIGHT);

                case WINTER ->
                    // 겨울 TOP 추천 규칙: LIGHT 제외
                    thickness != Thickness.LIGHT;
            };
            default ->
                // 기타 의류 타입 : 제한 없음
                true;
        };
    }
}