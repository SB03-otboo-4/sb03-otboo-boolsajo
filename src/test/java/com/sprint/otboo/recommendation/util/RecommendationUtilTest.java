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
import java.util.UUID;
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
        List<Clothes> recommended = engine.recommend(userClothes, perceivedTemp, weather);

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
        List<Clothes> recommended = engine.recommend(List.of(springTop), perceivedTemp, weather);

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
        List<Clothes> recommended = engine.recommend(List.of(summerTop), perceivedTemp, weather);

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
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather);

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
        List<Clothes> recommended = engine.recommend(List.of(winterScarf), perceivedTemp, weather);

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
        List<Clothes> recommended = recommendationEngine.recommend(List.of(fallOuterHeavy), perceivedTemp, weather);

        // then: 의상이 추천 목록에 포함되지 않음
        assertThat(recommended).doesNotContain(fallOuterHeavy);
    }
}