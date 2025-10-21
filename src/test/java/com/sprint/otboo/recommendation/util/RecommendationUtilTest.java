package com.sprint.otboo.recommendation.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.Season;
import com.sprint.otboo.recommendation.entity.TemperatureCategory;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RecommendationEngine + WeatherUtils 통합 테스트")
public class RecommendationUtilTest {

    private RecommendationEngine recommendationEngine;

    @BeforeEach
    void setUp() {
        recommendationEngine = new RecommendationEngineImpl();
    }

    @Test
    void 최고최저_온도_기준_일반_케이스() {
        // given: 최고/최저 온도, 풍속, 민감도
        double maxTemp = 25.0;
        double minTemp = 15.0;
        double windSpeed = 2.0;
        double windFactor = 1.0;
        int sensitivity = 2;

        // when: 체감 온도 계산
        double result = WeatherUtils.calculatePerceivedTemperature(maxTemp, minTemp, windSpeed, windFactor, sensitivity);

        // then: 결과 검증
        assertThat(result).isEqualTo(17.0); // 평균 20, 풍속 2 -> 18, 민감도 -1 -> 17
    }

    @Test
    void 최고최저_온도_기준_풍속0_테스트() {
        // given: 최고/최저 온도, 풍속 0, 민감도
        double maxTemp = 20;
        double minTemp = 10;
        double windSpeed = 0.0;
        double windFactor = 1.0;
        int sensitivity = 2;

        // when: 체감 온도 계산
        double result = WeatherUtils.calculatePerceivedTemperature(maxTemp, minTemp, windSpeed, windFactor, sensitivity);

        // then: 결과 검증
        assertThat(result).isEqualTo(14.0); // 평균 15, 풍속 0, 민감도 -1 -> 14
    }

    @Test
    void 현재_온도_기준_일반_케이스() {
        // given: 현재 온도, 풍속, 민감도
        double currentTemp = 20.0;
        double windSpeed = 2.0;
        double windFactor = 1.0;
        int sensitivity = 3;

        // when: 체감 온도 계산
        double result = WeatherUtils.calculatePerceivedTemperature(currentTemp, windSpeed, windFactor, sensitivity);

        // then: 결과 검증
        assertThat(result).isEqualTo(19.0); // 20-2+1
    }

    @Test
    void 현재_온도_기준_풍속0_테스트() {
        // given: 현재 온도, 풍속 0, 민감도
        double currentTemp = 15.0;
        double windSpeed = 0.0;
        double windFactor = 1.0;
        int sensitivity = 2;

        // when: 체감 온도 계산
        double result = WeatherUtils.calculatePerceivedTemperature(currentTemp, windSpeed, windFactor, sensitivity);

        // then: 결과 검증
        assertThat(result).isEqualTo(14.0); // 15-0+(-1)
    }

    @Test
    void 소수점_반올림_테스트() {
        // given: 최고/최저 온도, 풍속, 민감도
        double maxTemp = 20.666;
        double minTemp = 20.0;
        double windSpeed = 1.0;
        double windFactor = 1.0;
        int sensitivity = 2;

        // when: 체감 온도 계산
        double result = WeatherUtils.calculatePerceivedTemperature(maxTemp, minTemp, windSpeed, windFactor, sensitivity);

        // then: 소수점 반올림 결과 확인
        assertThat(result).isEqualTo(18.33); // 18.333 -> 반올림 18.33
    }

    @Test
    void 최고최저_현재_온도_계산_일관성_확인() {
        // given: 최고/최저 평균과 현재 온도, 풍속, 민감도
        double maxTemp = 20;
        double minTemp = 10;
        double windSpeed = 2.0;
        double windFactor = 1.0;
        int sensitivity = 2;

        // when: 두 계산 방식으로 체감 온도 계산
        double maxMin = WeatherUtils.calculatePerceivedTemperature(maxTemp, minTemp, windSpeed, windFactor, sensitivity);
        double current = WeatherUtils.calculatePerceivedTemperature(15, 2.0, 1.0, 2);

        // then: 두 계산 방식이 동일한지 검증
        assertThat(current).isEqualTo(maxMin);
    }

    @Test
    void 민감도_범위_벗어날_때_기본값_적용() {
        // given: 최고/최저 온도, 풍속, 민감도 범위 벗어남 (-1, 6)
        double maxTemp = 20.0;
        double minTemp = 10.0;
        double windSpeed = 2.0;
        double windFactor = 1.0;

        // when: 체감 온도 계산
        double resultLow = WeatherUtils.calculatePerceivedTemperature(maxTemp, minTemp, windSpeed, windFactor, -1);
        double resultHigh = WeatherUtils.calculatePerceivedTemperature(maxTemp, minTemp, windSpeed, windFactor, 6);

        // then: 안전하게 민감도 0 적용, 계산 결과 검증
        assertThat(resultLow).isEqualTo(13.0);
        assertThat(resultHigh).isEqualTo(13.0);
    }

    @Test
    void 최고기온이_최저기온보다_낮을_때() {
        // given: 최고 < 최저 온도, 풍속, 민감도
        double maxTemp = 5.0;
        double minTemp = 10.0;
        double windSpeed = 1.0;
        double windFactor = 1.0;
        int sensitivity = 2;

        // when: 체감 온도 계산
        double result = WeatherUtils.calculatePerceivedTemperature(maxTemp, minTemp, windSpeed, windFactor, sensitivity);

        // then: 평균 계산 정상, 음수 값 가능
        assertThat(result).isEqualTo(5.5);
    }

    @Test
    void 극단적인_풍속_값() {
        // given: 최고/최저 온도, 풍속 100 m/s, 민감도
        double maxTemp = 25.0;
        double minTemp = 15.0;
        double windSpeed = 100.0;
        double windFactor = 1.0;
        int sensitivity = 2;

        // when: 체감 온도 계산
        double result = WeatherUtils.calculatePerceivedTemperature(maxTemp, minTemp, windSpeed, windFactor, sensitivity);

        // then: 평균 20, 풍속 100 -> -80, 민감도 -1 -> -81
        assertThat(result).isEqualTo(-81.0);
    }

    @Test
    void 현재온도_체감_계산_예외() {
        // given: 현재 온도, 음수 풍속, 민감도
        double currentTemp = 10.0;
        double windSpeed = -5.0;
        double windFactor = 1.0;
        int sensitivity = 2;

        // when: 체감 온도 계산
        double result = WeatherUtils.calculatePerceivedTemperature(currentTemp, windSpeed, windFactor, sensitivity);

        // then: 음수 풍속 적용
        assertThat(result).isEqualTo(14.0); // 10 - (-5) -1 -> 14
    }

    @Test
    void 소수점_정확도_검증() {
        // given: 최고/최저 온도, 풍속, 민감도, 소수점 존재
        double maxTemp = 20.555;
        double minTemp = 20.444;
        double windSpeed = 0.111;
        double windFactor = 1.0;
        int sensitivity = 2;

        // when: 체감 온도 계산
        double result = WeatherUtils.calculatePerceivedTemperature(maxTemp, minTemp, windSpeed, windFactor, sensitivity);

        // then: 평균 20.4995, 풍속 보정 -0.111 -> 20.3885, 민감도 -1 -> 19.3885, 반올림 -> 19.39
        assertThat(result).isEqualTo(19.39);
    }

    @Test
    void 계절과_체감_온도에_따른_추천_의상() {
        // given: 테스트용 의상
        Clothes springTop = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(
                    null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "MEDIUM"
                )
            ))
            .build();

        List<Clothes> userClothes = List.of(springTop);

        // 날씨 객체
        Weather weather = Weather.builder()
            .maxC(17.0)
            .minC(16.0)
            .speedMs(3.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        // when: 체감 온도 계산
        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(),
            weather.getMinC(),
            weather.getSpeedMs(),
            0.8,
            2
        );

        // when: 추천 실행
        RecommendationEngine engine = new RecommendationEngineImpl();
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, true);

        // then: 의상이 추천 목록에 포함되는지 검증
        assertThat(recommended).contains(springTop);
    }

    @Test
    void 계절_분류_경계값() {
        // given: 경계값 체감 온도
        double springStart = 15.0;
        double springEnd = 22.9;
        double summerStart = 23.0;
        double fallStart = 7.0;
        double winterStart = 6.9;

        // when & then: 계절 분류
        assertThat(WeatherUtils.classifySeason(springStart)).isEqualTo(Season.SPRING);
        assertThat(WeatherUtils.classifySeason(springEnd)).isEqualTo(Season.SPRING);
        assertThat(WeatherUtils.classifySeason(summerStart)).isEqualTo(Season.SUMMER);
        assertThat(WeatherUtils.classifySeason(fallStart)).isEqualTo(Season.FALL);
        assertThat(WeatherUtils.classifySeason(winterStart)).isEqualTo(Season.WINTER);
    }

    @Test
    void 세부_온도_범주_경계값() {
        // given: 계절별 경계값 체감 온도
        double springLow = 17.4;
        double springHigh = 17.5;
        double fallLow = 12.9;
        double fallHigh = 13.0;
        double summerLow = 28.0;
        double summerHigh = 28.1;
        double winterLow = 0.0;
        double winterHigh = 0.1;

        // when & then: 세부 온도 범주 판별
        assertThat(WeatherUtils.classifyTemperatureCategory(Season.SPRING, springLow))
            .isEqualTo(TemperatureCategory.LOW);
        assertThat(WeatherUtils.classifyTemperatureCategory(Season.SPRING, springHigh))
            .isEqualTo(TemperatureCategory.HIGH);

        assertThat(WeatherUtils.classifyTemperatureCategory(Season.FALL, fallLow))
            .isEqualTo(TemperatureCategory.LOW);
        assertThat(WeatherUtils.classifyTemperatureCategory(Season.FALL, fallHigh))
            .isEqualTo(TemperatureCategory.HIGH);

        assertThat(WeatherUtils.classifyTemperatureCategory(Season.SUMMER, summerLow))
            .isEqualTo(TemperatureCategory.LOW);
        assertThat(WeatherUtils.classifyTemperatureCategory(Season.SUMMER, summerHigh))
            .isEqualTo(TemperatureCategory.HIGH);

        assertThat(WeatherUtils.classifyTemperatureCategory(Season.WINTER, winterLow))
            .isEqualTo(TemperatureCategory.LOW);
        assertThat(WeatherUtils.classifyTemperatureCategory(Season.WINTER, winterHigh))
            .isEqualTo(TemperatureCategory.HIGH);
    }

    @Test
    void 일교차_계산() {
        // given: 최고기온과 최저기온
        double maxTemp = 20.0;
        double minTemp = 10.0;
        double reverseMax = 10.0;
        double reverseMin = 20.0;

        // when & then: 일교차 계산
        assertThat(WeatherUtils.calculateDailyRange(maxTemp, minTemp)).isEqualTo(10.0);
        assertThat(WeatherUtils.calculateDailyRange(reverseMax, reverseMin)).isEqualTo(-10.0); // 예외적 케이스
    }

    @Test
    void 체감온도_풍속0_민감도0() {
        // given: 현재 체감 온도 계산 조건
        double maxTemp = 20.0;
        double minTemp = 10.0;
        double windSpeed = 0.0;
        double windFactor = 1.0;
        int sensitivity = 0;

        // when: 체감 온도 계산
        double result = WeatherUtils.calculatePerceivedTemperature(maxTemp, minTemp, windSpeed, windFactor, sensitivity);

        // then: 풍속 0, 민감도 0 적용된 결과 검증
        assertThat(result).isEqualTo(12.0); // 평균 15, 풍속 0, 민감도 -3
    }

    @Test
    void 체감온도_입력_예외() {
        // given: NaN 값 입력
        double maxTemp = Double.NaN;
        double minTemp = 20.0;
        double windSpeed = 2.0;
        double windFactor = 1.0;
        int sensitivity = 2;

        // when: NaN 값은 0.0으로 안전 처리
        double safeMax = Double.isNaN(maxTemp) ? 0.0 : maxTemp;
        double safeMin = Double.isNaN(minTemp) ? 0.0 : minTemp;

        // then: 계산 시 예외 없이 정상 실행
        assertThatCode(() ->
            WeatherUtils.calculatePerceivedTemperature(safeMax, safeMin, windSpeed, windFactor, sensitivity)
        ).doesNotThrowAnyException();
    }


    @Test
    void 체감온도_풍속음수_민감도최대_최소_조합() {
        // given: 음수 풍속, 민감도 극단값
        double currentTemp = 20.0;
        double windSpeed = -10.0;

        // when & then: 민감도 0~5 조합
        for (int sensitivity = 0; sensitivity <= 5; sensitivity++) {
            double result = WeatherUtils.calculatePerceivedTemperature(currentTemp, windSpeed, 1.0, sensitivity);
            assertThat(result).isNotNaN();
        }
    }

    @Test
    void 계절_세부온도범주_여러의상_추천() {
        // given: 다양한 의상
        Clothes lightTop = Clothes.builder().id(UUID.randomUUID()).type(ClothesType.TOP)
            .attributes(List.of(ClothesAttribute.create(null,
                ClothesAttributeDef.builder().name("thickness").build(), "LIGHT")))
            .build();
        Clothes heavyTop = Clothes.builder().id(UUID.randomUUID()).type(ClothesType.TOP)
            .attributes(List.of(ClothesAttribute.create(null,
                ClothesAttributeDef.builder().name("thickness").build(), "HEAVY")))
            .build();
        List<Clothes> userClothes = List.of(lightTop, heavyTop);

        // 날씨: 봄, 체감온도 직접 지정
        Weather weather = Weather.builder()
            .maxC(19.0)
            .minC(17.0)
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .build();

        // when: 추천할 체감온도를 직접 지정하여 테스트
        double perceivedTemp = 18.0;
        List<Clothes> recommended = new RecommendationEngineImpl().recommend(userClothes, perceivedTemp, weather, true);

        // then: 추천된 의상 ID 기준으로 검증
        List<UUID> recommendedIds = recommended.stream().map(Clothes::getId).toList();
        assertThat(recommendedIds).contains(lightTop.getId());
        assertThat(recommendedIds).doesNotContain(heavyTop.getId());
    }

    @Test
    void 체감온도_반올림_일관성() {
        // given: 다양한 풍속과 민감도
        double[] windSpeeds = {0.0, 1.0, -1.0, 5.5};
        int[] sensitivities = {0, 2, 5};

        for (double wind : windSpeeds) {
            for (int sens : sensitivities) {
                // when: 체감온도 계산
                double result = WeatherUtils.calculatePerceivedTemperature(20.333, 19.777, wind, 1.0, sens);

                // then: 소수점 두 자리 일관성 확인
                String formatted = String.format("%.2f", result);
                assertThat(formatted).matches("\\d+\\.\\d{2}");
            }
        }
    }

    @Test
    void 일교차_극단값() {
        // given: 극단값
        double maxTemp = 50.0;
        double minTemp = -50.0;

        // when & then
        assertThat(WeatherUtils.calculateDailyRange(maxTemp, minTemp)).isEqualTo(100.0);
        assertThat(WeatherUtils.calculateDailyRange(minTemp, maxTemp)).isEqualTo(-100.0);
        assertThat(WeatherUtils.calculateDailyRange(maxTemp, maxTemp)).isEqualTo(0.0);
    }

    @Test
    void 봄_체감온도_LOW_TOP_추천() {
        // given: 의상과 날씨
        Clothes springTop = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "MEDIUM")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(17.0)
            .minC(15.0)
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2);

        RecommendationEngine engine = new RecommendationEngineImpl();

        // when: 체감 온도 계산 및 추천 실행
        List<Clothes> recommended = engine.recommend(List.of(springTop), perceivedTemp, weather, true);

        // then: 추천 목록에 의상 포함
        assertThat(recommended).contains(springTop);
    }

    @Test
    void 여름_TOP_추천_맑은_날() {
        // given: 여름용 의상과 날씨
        Clothes summerTop = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "LIGHT")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(25.0)
            .minC(23.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        // when: 체감 온도 계산 및 추천 실행
        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 3);
        RecommendationEngine engine = new RecommendationEngineImpl();
        List<Clothes> recommended = engine.recommend(List.of(summerTop), perceivedTemp, weather, true);

        // then: 추천 목록에 의상 포함
        assertThat(recommended).contains(summerTop);
    }

    @Test
    void 가을_OUTER_일교차_6도_이상_강제_추천() {
        // given: 가을용 외투와 날씨(일교차 >= 6)
        Clothes fallOuter = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.OUTER)
            .attributes(List.of(
                ClothesAttribute.create(
                    null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "MEDIUM"
                )
            ))
            .build();
        List<Clothes> userClothes = List.of(fallOuter);

        Weather weather = Weather.builder()
            .maxC(15.0)
            .minC(8.0) // 일교차 7 >= 6
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .build();

        // when: 체감 온도 계산 및 추천 실행
        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(),
            weather.getSpeedMs(), 0.8,
            2
        );
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, true);

        // then: 외투가 추천 목록에 포함
        assertThat(recommended).contains(fallOuter);
    }

    @Test
    void 겨울_SCARF_추천() {
        // given: 겨울용 스카프와 날씨
        Clothes winterScarf = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.SCARF)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "HEAVY")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(0.0)
            .minC(-5.0)
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.SNOW)
            .build();

        // when: 체감 온도 계산 및 추천 실행
        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2);

        RecommendationEngine engine = new RecommendationEngineImpl();
        List<Clothes> recommended = engine.recommend(List.of(winterScarf), perceivedTemp, weather, true);

        // then: 스카프가 추천 목록에 포함
        assertThat(recommended).contains(winterScarf);
    }

    @Test
    void 가을_OUTER_두꺼움_HIGH_구간_추천되지_않음() {
        // given: 두꺼운 가을용 외투와 날씨
        Clothes fallOuterHeavy = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.OUTER)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "HEAVY")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(18.0)
            .minC(15.0)
            .speedMs(6.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .build();

        // when: 체감 온도 계산 및 추천 실행
        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );
        List<Clothes> recommended = recommendationEngine.recommend(List.of(fallOuterHeavy), perceivedTemp, weather, true);

        // then: 의상이 추천 목록에 포함되지 않음
        assertThat(recommended).doesNotContain(fallOuterHeavy);
    }

    @Test
    void OUTER_강제추천_조건일교차4이상_풍속3이상_구름많음() {
        // given: OUTER 의상과 날씨
        Clothes outer = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.OUTER)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "MEDIUM")
            ))
            .build();
        List<Clothes> userClothes = List.of(outer);
        Weather weather = Weather.builder()
            .maxC(15.0)
            .minC(10.0)
            .speedMs(3.5)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .build();

        // when: 체감 온도 계산 및 추천 실행
        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, true);

        // then: 외투가 추천 목록에 포함되어야 함
        assertThat(recommended).contains(outer);
    }

    @Test
    void 계절속성_영문_추천_필터적용() {
        // given: 계절 속성 의상과 날씨
        Clothes springTop = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("season").build(),
                    "SPRING")
            ))
            .build();
        Weather weather = Weather.builder()
            .maxC(17.0)
            .minC(15.0)
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();
        double perceivedTemp = 16.0; // SPRING 범위 강제

        // when: 추천 실행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(springTop), perceivedTemp, weather,true);

        // then: 의상이 추천 목록에 포함
        assertThat(recommended).extracting(Clothes::getId).contains(springTop.getId());
    }

    @Test
    void 계절속성_한글_추천_필터적용() {
        // given: 계절 속성 의상(한글)과 날씨
        Clothes springTop = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("계절").build(),
                    "봄")
            ))
            .build();
        Weather weather = Weather.builder()
            .maxC(17.0)
            .minC(15.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();
        double perceivedTemp = 16.0; // SPRING으로 분류

        // when: 추천 실행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(springTop), perceivedTemp, weather, true);

        // then: 의상이 추천 목록에 포함
        assertThat(recommended).extracting(Clothes::getId).contains(springTop.getId());
    }

    @Test
    void 두께속성_영문_추천_필터적용() {
        // given: 두께 속성 의상과 날씨
        Clothes topWithThickness = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "MEDIUM")
            ))
            .build();
        List<Clothes> userClothes = List.of(topWithThickness);
        Weather weather = Weather.builder()
            .maxC(17.0)
            .minC(15.0)
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();
        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 실행
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, true);

        // then: 의상이 추천 목록에 포함
        assertThat(recommended).contains(topWithThickness);
    }

    @Test
    void 두께속성_한글_추천_필터적용() {
        // given: 두께 속성 의상(한글)과 날씨
        Clothes mediumTop = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("두께").build(),
                    "보통")
            ))
            .build();
        Weather weather = Weather.builder()
            .maxC(17.0)
            .minC(15.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();
        double perceivedTemp = 16.0;

        // when: 추천 실행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(mediumTop), perceivedTemp, weather, true);

        // then: 의상이 추천 목록에 포함
        assertThat(recommended).extracting(Clothes::getId).contains(mediumTop.getId());
    }

    @Test
    void 계절_두께_속성_추천_필터적용() {
        // given: 계절 + 두께 속성 의상과 날씨
        Clothes springMediumTop = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("season").build(),
                    "SPRING"),
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("두께").build(),
                    "보통")
            ))
            .build();
        Weather weather = Weather.builder()
            .maxC(17.0)
            .minC(15.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();
        double perceivedTemp = 16.0;

        // when: 추천 실행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(springMediumTop), perceivedTemp, weather, true);

        // then: 의상이 추천 목록에 포함
        assertThat(recommended).extracting(Clothes::getId).contains(springMediumTop.getId());
    }

    @Test
    void 계절속성_불일치_추천제외() {
        // given: 계절 속성 의상과 날씨
        Clothes summerTop = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("season").build(),
                    "SUMMER")
            ))
            .build();
        Weather weather = Weather.builder()
            .maxC(16.0)
            .minC(15.0)
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();
        double perceivedTemp = 16.0; // SPRING 범위

        // when: 추천 실행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(summerTop), perceivedTemp, weather, true);

        // then: 의상이 추천 목록에 포함되지 않음
        assertThat(recommended).extracting(Clothes::getId).doesNotContain(summerTop.getId());
    }

    @Test
    void OUTER_규칙_테스트_일교차_풍속_구름많음() {
        // given: OUTER 의상과 날씨
        Clothes outer = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.OUTER)
            .attributes(List.of(
                ClothesAttribute.create(null, null, "MEDIUM")
            ))
            .build();

        List<Clothes> userClothes = List.of(outer);

        Weather weather = Weather.builder()
            .maxC(15.0)
            .minC(10.0)    // 일교차 5
            .speedMs(3.5)  // 풍속 >= 3
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 알고리즘 실행
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, true);

        // then: 외투가 추천 목록에 포함되어야 함 (OUTER 규칙)
        assertThat(recommended).contains(outer);
    }

    @Test
    void 외투_추천_가을_HIGH_MEDIUM() {
        // given: FALL HIGH 체감온도용 OUTER 의상
        Clothes outer = Clothes.builder()
            .type(ClothesType.OUTER)
            .attributes(List.of(
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("thickness").build(), "MEDIUM"),
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("season").build(), "FALL")
            ))
            .build();

        List<Clothes> userClothes = List.of(outer);

        Weather weather = Weather.builder()
            .maxC(18.0)  // FALL HIGH 체감온도 범위
            .minC(16.0)
            .speedMs(1.0) // 강제 추천 조건 아님
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        // when: 체감 온도 계산 및 추천
        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 1.0, 0
        );
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, true);

        // then: 기본 추천 규칙 통과 확인
        assertThat(recommended).contains(outer);
    }

    @Test
    void 외투_추천_스프링_HIGH_MEDIUM_기본추천() {
        // given: OUTER 의상
        Clothes outer = Clothes.builder()
            .type(ClothesType.OUTER)
            .attributes(List.of(
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("thickness").build(), "MEDIUM"),
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("season").build(), "SPRING")
            ))
            .build();

        List<Clothes> userClothes = List.of(outer);

        Weather weather = Weather.builder()
            .maxC(20.0)  // SPRING HIGH
            .minC(18.0)
            .speedMs(1.0) // 강제 추천 조건 X
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        // when
        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 1.0, 0
        );
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, true);

        // then: 기본 추천 규칙 통과 확인
        assertThat(recommended).contains(outer);
    }

    @Test
    void 봄낮은체감온도_맑은날_드레스추천() {
        // given: SPRING LOW 체감온도용 DRESS
        Clothes springDress = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.DRESS)
            .attributes(List.of(
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("thickness").build(), "MEDIUM"),
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("season").build(), "SPRING")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(18.0)
            .minC(16.0)
            .speedMs(0.5)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 수행
        List<Clothes> recommended = recommendationEngine.recommend(
            List.of(springDress),
            perceivedTemp,
            weather,
            false
        );

        // then: 추천 결과에 SPRING LOW 드레스 포함
        assertThat(recommended)
            .usingRecursiveFieldByFieldElementComparator()
            .contains(springDress);
    }

    @Test
    void 봄높은체감온도_강풍제외() {
        // given: SPRING HIGH 체감온도용 DRESS
        Clothes springDress = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.DRESS)
            .attributes(List.of(
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("thickness").build(), "LIGHT"),
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("season").build(), "SPRING")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(22.0)
            .minC(20.0)
            .speedMs(5.5) // 풍속 > 5 -> 제외
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 수행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(springDress), perceivedTemp, weather, false);

        // then: 추천 결과에 포함되지 않음
        assertThat(recommended).doesNotContain(springDress);
    }

    @Test
    void 여름비_풍속제외() {
        // given: SUMMER DRESS
        Clothes summerDress = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.DRESS)
            .attributes(List.of(
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("thickness").build(), "LIGHT"),
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("season").build(), "SUMMER")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(28.0)
            .minC(25.0)
            .speedMs(3.0) // 풍속 3
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.RAIN)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 수행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(summerDress), perceivedTemp, weather, false);

        // then: 추천 결과에 포함되지 않음
        assertThat(recommended).doesNotContain(summerDress);
    }

    @Test
    void 가을낮은체감온도_눈제외() {
        // given: FALL LOW 체감온도 DRESS
        Clothes fallDress = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.DRESS)
            .attributes(List.of(
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("thickness").build(), "HEAVY"),
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("season").build(), "FALL")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(12.0)
            .minC(7.0)
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.SNOW)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 수행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(fallDress), perceivedTemp, weather, false);

        // then: 추천 결과에 포함되지 않음
        assertThat(recommended).doesNotContain(fallDress);
    }

    @Test
    void 겨울_눈제외() {
        // given: WINTER DRESS
        Clothes winterDress = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.DRESS)
            .attributes(List.of(
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("thickness").build(), "HEAVY"),
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("season").build(), "WINTER")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(-2.0)
            .minC(-6.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.SNOW)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 수행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(winterDress), perceivedTemp, weather, false);

        // then: 추천 결과에 포함되지 않음
        assertThat(recommended).doesNotContain(winterDress);
    }

    @Test
    void 여름맑은날_드레스추천() {
        // given: SUMMER DRESS
        Clothes summerDress = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.DRESS)
            .attributes(List.of(
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("thickness").build(), "LIGHT"),
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("season").build(), "SUMMER")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(30.0)
            .minC(25.0)
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 수행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(summerDress), perceivedTemp, weather, false);

        // then: 추천 결과에 포함
        assertThat(recommended).contains(summerDress);
    }

    @Test
    void HAT_규칙_테스트_봄_HIGH_맑은날() {
        // given: HAT 의상과 봄 HIGH 날씨 정보
        Clothes hat = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.HAT)
            .build();

        List<Clothes> userClothes = List.of(hat);

        Weather weather = Weather.builder()
            .maxC(20.0)
            .minC(15.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = 19.0; // 봄 HIGH 구간

        // when: 추천 알고리즘 실행
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, true);

        // then: HAT 규칙에 따라 추천되어야 함
        assertThat(recommended).contains(hat);
    }

    @Test
    void SCARF_규칙_테스트_겨울_LOW_눈오는날() {
        // given: SCARF 의상과 겨울 LOW 날씨 정보
        Clothes scarf = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.SCARF)
            .attributes(List.of(
                ClothesAttribute.create(null, null, "HEAVY")
            ))
            .build();

        List<Clothes> userClothes = List.of(scarf);

        Weather weather = Weather.builder()
            .maxC(-1.0)
            .minC(-5.0)
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.SNOW)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 알고리즘 실행
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, true);

        // then: SCARF 규칙에 따라 추천되어야 함
        assertThat(recommended).contains(scarf);
    }

    @Test
    void OUTER_HAT_SCARF_통합_추천_테스트() {
        // given: OUTER, HAT, SCARF 의상과 겨울 날씨 정보
        Clothes outer = Clothes.builder().id(UUID.randomUUID()).type(ClothesType.OUTER).build();
        Clothes hat = Clothes.builder().id(UUID.randomUUID()).type(ClothesType.HAT).build();
        Clothes scarf = Clothes.builder().id(UUID.randomUUID()).type(ClothesType.SCARF).build();

        List<Clothes> userClothes = List.of(outer, hat, scarf);

        // 날씨: 겨울 LOW, 눈 + 구름
        Weather weather = Weather.builder()
            .maxC(-1.0)
            .minC(-4.0)
            .speedMs(3.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.SNOW)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 알고리즘 실행
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, true);

        // then: 모든 타입이 규칙에 따라 추천되어야 함
        assertThat(recommended).containsExactlyInAnyOrder(outer, hat, scarf);
    }

    @Test
    void 의상속성없는경우_기본추천통과() {
        // given: 속성 없는 기본 의상과 날씨
        Clothes basic = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .build();

        Weather weather = Weather.builder()
            .maxC(20.0)
            .minC(15.0)
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = 18.0;

        // when: 추천 엔진 호출
        List<Clothes> recommended = recommendationEngine.recommend(List.of(basic), perceivedTemp, weather, false);

        // then: 추천 결과에 포함
        assertThat(recommended).contains(basic);
    }

    @Test
    void 의상_계절속성_잘못된_경우() {
        // given: 계절 속성이 INVALID인 의상
        Clothes unknownSeason = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("season").build(), "INVALID")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(25.0)
            .minC(20.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = 22.0; // SPRING

        // when: 추천 엔진 호출
        List<Clothes> recommended = recommendationEngine.recommend(List.of(unknownSeason), perceivedTemp, weather, false);

        // then: 추천 통과
        assertThat(recommended).contains(unknownSeason);
    }

    @Test
    void 드레스_제외_플래그_동작확인() {
        // given: DRESS와 TOP 의상, DRESS 제외 플래그
        Clothes dress = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.DRESS)
            .build();

        Clothes top = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .build();

        Weather weather = Weather.builder()
            .maxC(30.0)
            .minC(25.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = 28.0; // SUMMER

        // when: 추천 엔진 호출
        List<Clothes> recommended = recommendationEngine.recommend(List.of(dress, top), perceivedTemp, weather, true);

        // then: DRESS 제외, TOP 포함
        assertThat(recommended).doesNotContain(dress);
        assertThat(recommended).contains(top);
    }

    @Test
    void 아우터_일교차_강제추천() {
        // given: OUTER 의상, 봄 계절, 일교차 7도
        Clothes outer = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.OUTER)
            .build();

        Weather weather = Weather.builder()
            .maxC(25.0)
            .minC(18.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = 20.0; // SPRING, LOW/HIGH 상관없이 일교차 7도

        // when: 추천 엔진 호출
        List<Clothes> recommended = recommendationEngine.recommend(List.of(outer), perceivedTemp, weather, false);

        // then: 추천 결과에 포함
        assertThat(recommended).contains(outer);
    }

    @Test
    void 두께규칙위반_추천제외() {
        // given: WINTER, LOW, LIGHT 두께 TOP
        Clothes winterTop = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "LIGHT")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(-1.0)
            .minC(-5.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.SNOW)
            .build();

        double perceivedTemp = -3.0; // WINTER, LOW

        // when: 추천 엔진 호출
        List<Clothes> recommended = recommendationEngine.recommend(List.of(winterTop), perceivedTemp, weather, false);

        // then: 추천에서 제외
        assertThat(recommended).doesNotContain(winterTop);
    }

    @Test
    void 날씨정보Null_안전처리() {
        // given: 날씨 DTO null 필드, 기본 TOP 의상
        Clothes top = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .build();

        Weather weather = Weather.builder()
            .maxC(null)
            .minC(null)
            .speedMs(null)
            .skyStatus(null)
            .type(null)
            .build();

        double perceivedTemp = 20.0;

        // when: 추천 엔진 호출
        List<Clothes> recommended = recommendationEngine.recommend(List.of(top), perceivedTemp, weather, false);

        // then: 추천 결과 포함
        assertThat(recommended).contains(top);
    }

    @Test
    void 의상리스트_빈경우_추천결과_빈_리스트() {
        // given: 빈 의상 리스트와 기본 날씨
        List<Clothes> userClothes = List.of();
        Weather weather = Weather.builder()
            .maxC(20.0)
            .minC(15.0)
            .speedMs(2.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();
        double perceivedTemp = 18.0;

        // when: 추천 실행
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, false);

        // then: 결과는 빈 리스트
        assertThat(recommended).isEmpty();
    }

    @Test
    void 의상_속성_null_경우_기본추천_통과() {
        // given: season과 thickness 모두 null 의상과 날씨
        Clothes basic = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null, null, null)
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(18.0)
            .minC(15.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = 16.0;

        // when: 추천 실행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(basic), perceivedTemp, weather, false);

        // then: 기본 추천 통과
        assertThat(recommended).contains(basic);
    }

    @Test
    void 체감온도_경계값_LOW_High_검증() {
        // given: LOW/HIGH 경계값에 해당하는 SPRING OUTER 의상과 날씨 (일교차 6도 이상)
        Clothes outer = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.OUTER)
            .attributes(List.of(
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("season").build(), "SPRING"),
                ClothesAttribute.create(null, ClothesAttributeDef.builder().name("thickness").build(), "MEDIUM")
            ))
            .build();

        Weather weatherLow = Weather.builder()
            .maxC(20.0)
            .minC(14.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTempLow = WeatherUtils.calculatePerceivedTemperature(
            weatherLow.getMaxC(), weatherLow.getMinC(), weatherLow.getSpeedMs(), 0.8, 2
        );

        // when: 추천 엔진 실행
        List<Clothes> recommendedLow = recommendationEngine.recommend(List.of(outer), perceivedTempLow, weatherLow, false);

        // then: LOW/HIGH 경계값 의상이 추천 목록에 포함되어야 함
        assertThat(recommendedLow).contains(outer);
    }

    @Test
    void 강제추천조건_일교차풍속_겹치는_경우() {
        // given: 일교차 6 이상, 풍속 3 이상 조건의 OUTER 의상
        Clothes outer = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.OUTER)
            .attributes(List.of(
                ClothesAttribute.create(null, null, "MEDIUM")
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(20.0)
            .minC(13.0) // 일교차 7
            .speedMs(3.5)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 실행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(outer), perceivedTemp, weather, false);

        // then: 강제추천 포함 확인
        assertThat(recommended).contains(outer);
    }

    @Test
    void OUTER_속성_null_강제추천_일교차조건() {
        // given: 속성 null인 OUTER 의상과 일교차 7도 조건
        Clothes outer = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.OUTER)
            .attributes(List.of(
                ClothesAttribute.create(null, null, null)
            ))
            .build();

        Weather weather = Weather.builder()
            .maxC(22.0)
            .minC(15.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .build();

        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        // when: 추천 실행
        List<Clothes> recommended = recommendationEngine.recommend(List.of(outer), perceivedTemp, weather, false);

        // then: 속성이 없어도 강제추천 포함 확인
        assertThat(recommended).contains(outer);
    }

    @Test
    void 모든타입_추천_및_제외_통합_첫번째선택() {
        // given: 의상 타입별 테스트용 의상 생성
        Clothes topSpring = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("계절").build(),
                    "봄"),                       // 계절: SPRING -> 체감 온도 SPRING과 일치
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "MEDIUM")                     // 두께: 적합
            ))
            .build();

        Clothes topSummer = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.TOP)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("계절").build(),
                    "SUMMER"),                     // 계절: SUMMER -> 제외 대상
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "LIGHT")
            ))
            .build();

        Clothes bottomSpring = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.BOTTOM)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("season").build(),
                    "SPRING")                     // 계절: SPRING -> 추천 대상
            ))
            .build();

        Clothes bottomWinter = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.BOTTOM)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("season").build(),
                    "WINTER")                     // 계절: WINTER -> 제외 대상
            ))
            .build();

        Clothes outer = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.OUTER)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("두께").build(),
                    "중간")                       // OUTER: 일교차 강제 추천
            ))
            .build();

        Clothes hat = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.HAT)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("season").build(),
                    "SUMMER")                     // 계절: SUMMER -> SPRING 체감 온도에서 제외
            ))
            .build();

        Clothes scarf = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.SCARF)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("thickness").build(),
                    "HEAVY")                       // 계절/두께 규칙에 의해 제외
            ))
            .build();

        Clothes shoes = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.SHOES)
            .attributes(List.of(
                ClothesAttribute.create(null,
                    ClothesAttributeDef.builder().name("두께").build(),
                    "LIGHT")                       // 두께 제한 없음
            ))
            .build();

        Clothes accessory = Clothes.builder()
            .id(UUID.randomUUID())
            .type(ClothesType.ACCESSORY)
            .attributes(List.of())                  // 항상 추천 가능
            .build();

        List<Clothes> userClothes = List.of(
            topSpring, topSummer,
            bottomSpring, bottomWinter,
            outer, hat, scarf, shoes, accessory
        );

        // 날씨: SPRING, 체감 온도 LOW 구간, 일교차 7°C, 풍속 1 m/s
        Weather weather = Weather.builder()
            .maxC(20.0)
            .minC(14.0)
            .speedMs(1.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .build();

        // 체감 온도 계산 (SPRING, LOW 구간)
        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(), weather.getSpeedMs(), 0.8, 2
        );

        RecommendationEngine engine = new RecommendationEngineImpl();

        // when: 추천 실행
        List<Clothes> recommended = engine.recommend(userClothes, perceivedTemp, weather, true);

        // then: 필터링 결과 확인
        Set<UUID> recommendedIds = recommended.stream()
            .map(Clothes::getId)
            .collect(Collectors.toSet());

        // 추천되어야 하는 의상
        assertThat(recommendedIds).contains(topSpring.getId());      // TOP: 계절 일치
        assertThat(recommendedIds).contains(bottomSpring.getId());   // BOTTOM: 계절 일치
        assertThat(recommendedIds).contains(outer.getId());          // OUTER: 일교차 강제 추천
        assertThat(recommendedIds).contains(shoes.getId());          // SHOES: 제한 없음
        assertThat(recommendedIds).contains(accessory.getId());      // ACCESSORY: 제한 없음

        // 추천 제외 의상
        assertThat(recommendedIds).doesNotContain(topSummer.getId());   // TOP: 계절 불일치
        assertThat(recommendedIds).doesNotContain(bottomWinter.getId());// BOTTOM: 계절 불일치
        assertThat(recommendedIds).doesNotContain(hat.getId());         // HAT: 계절 불일치
        assertThat(recommendedIds).doesNotContain(scarf.getId());       // SCARF: 계절/두께 규칙에 의해 제외

        // 타입별로 1개씩만 포함되는지 확인
        Map<ClothesType, Long> typeCount = recommended.stream()
            .collect(Collectors.groupingBy(Clothes::getType, Collectors.counting()));
        typeCount.forEach((type, count) -> assertThat(count).isEqualTo(1));
    }
}