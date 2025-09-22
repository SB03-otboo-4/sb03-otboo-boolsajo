package com.sprint.otboo.recommendation.util;

import com.sprint.otboo.clothing.entity.attribute.Season;
import com.sprint.otboo.recommendation.entity.TemperatureCategory;

/**
 * 날씨 관련 유틸리티 클래스
 * <p>
 * 체감 온도 계산, 계절 분류, 세부 온도 범주 분류, 일교차 계산 등 제공
 */
public class WeatherUtils {

    /**
     * 체감 온도 계산
     *
     * @param maxTemp 최고 기온 (°C)
     * @param minTemp 최저 기온 (°C)
     * @param windSpeed 풍속 (m/s)
     * @param windFactor 바람 세기 보정 계수 (기본 1.0)
     * @param sensitivity 개인 온도 민감도 (0~5, 양수: 더 덥게, 음수: 더 춥게)
     * @return 체감 온도 (°C)
     */
    public static double calculatePerceivedTemperature(
        double maxTemp, double minTemp, double windSpeed,
        double windFactor, int sensitivity
    ) {
        // 1. 평균 기온
        double avgTemp = (maxTemp + minTemp) / 2.0;

        // 2. 풍속 보정
        double windChill = avgTemp - (windSpeed * windFactor);

        // 3. 개인 온도 민감도 조정( 추위 0 ~ 2 <- ± -> 더위 3 ~ 5 )
        int adjustedSensitivity = switch (sensitivity) {
            case 0 -> -3;
            case 1 -> -2;
            case 2 -> -1;
            case 3 -> 1;
            case 4 -> 2;
            case 5 -> 3;
            default -> 0; // 안전하게 기본값
        };

        // 4. 최종 체감 온도
        return windChill + adjustedSensitivity;
    }

    /**
     * 체감 온도를 기준으로 통합 계절 판별
     *
     * @param perceivedTemp 체감 온도 (°C)
     * @return 계절(Season) enum
     */
    public static Season classifySeason(double perceivedTemp) {
        if (perceivedTemp >= 15.0 && perceivedTemp < 23.0) {
            return Season.SPRING;
        } else if (perceivedTemp >= 23.0) {
            return Season.SUMMER;
        } else if (perceivedTemp >= 7.0) {
            return Season.FALL;
        } else {
            return Season.WINTER;
        }
    }

    /**
     * 계절과 체감 온도를 기준으로 세부 온도 범주(Low/High) 판별
     *
     * @param season 계절(Season)
     * @param perceivedTemp 체감 온도 (°C)
     * @return TemperatureCategory enum (LOW / HIGH)
     */
    public static TemperatureCategory classifyTemperatureCategory(Season season, double perceivedTemp) {
        return switch (season) {
            case SPRING
                // 봄(15~23°C) 구간 세분화
                // 15.0 ~ 17.4°C -> "LOW" 구간 (조금 쌀쌀)
                // 17.5 ~ 22.9°C -> "HIGH" 구간 (따뜻)
                -> (perceivedTemp <= 17.4) ? TemperatureCategory.LOW : TemperatureCategory.HIGH;

            case FALL
                // 가을(7~15°C) 구간 세분화
                // 7.0 ~ 12.9°C -> "LOW" 구간 (서늘)
                // 13.0 ~ 14.9°C -> "HIGH" 구간 (쾌적)
                -> (perceivedTemp <= 12.9) ? TemperatureCategory.LOW : TemperatureCategory.HIGH;


            case SUMMER
                // 여름(23°C 이상) 구간 세분화
                // 23.0 ~ 28.0°C -> "LOW" 구간 (적당히 따뜻)
                // 28.1°C 이상 -> "HIGH" 구간 (덥게 느낌)
                -> (perceivedTemp <= 28.0) ? TemperatureCategory.LOW : TemperatureCategory.HIGH;

            case WINTER
                // 겨울(-∞ ~ 7°C) 구간 세분화
                // -∞ ~ 6.9°C -> "LOW" 구간 (춥게 느낌)
                // 7.0°C 이상 -> "HIGH" 구간 (덜 춥게 느낌)
                -> (perceivedTemp <= 6.9) ? TemperatureCategory.LOW : TemperatureCategory.HIGH;
        };
    }

    /**
     * 하루 기온 범위(최고 기온 - 최저 기온) 계산
     *
     * @param maxTemp 최고 기온 (°C)
     * @param minTemp 최저 기온 (°C)
     * @return 일교차 (°C)
     */
    public static double calculateDailyRange(double maxTemp, double minTemp) {
        // 단순 일교차 계산
        return maxTemp - minTemp;
    }
}