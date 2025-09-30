package com.sprint.otboo.recommendation.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("RecommendationEngine + WeatherUtils 통합 테스트")
public class RecommendationUtilTest {

    @Autowired
    private RecommendationEngine recommendationEngine;

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