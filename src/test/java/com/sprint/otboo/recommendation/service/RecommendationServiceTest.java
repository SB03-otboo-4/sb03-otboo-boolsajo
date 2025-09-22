package com.sprint.otboo.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.Season;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.entity.Recommendation;
import com.sprint.otboo.recommendation.entity.TemperatureCategory;
import com.sprint.otboo.recommendation.mapper.RecommendationMapper;
import com.sprint.otboo.recommendation.repository.RecommendationRepository;
import com.sprint.otboo.recommendation.util.RecommendationEngine;
import com.sprint.otboo.recommendation.util.WeatherUtils;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.entity.UserProfile;
import com.sprint.otboo.user.repository.UserProfileRepository;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WindStrength;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("의상 추천 서비스 테스트")
public class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationMapper recommendationMapper;

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private RecommendationEngine recommendationEngine;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private UUID userId;
    private UUID weatherId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        weatherId = UUID.randomUUID();
    }

    @Test
    void 추천_조회_성공_기본_타입_규칙() {
        // given: 날씨 정보, 사용자의 의상, 사용자 프로필 존재
        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(28.0)
            .maxC(30.0)
            .minC(25.0)
            .asWord(WindStrength.WEAK)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .speedMs(2.0)
            .build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        Clothes clothes = Clothes.builder()
            .id(UUID.randomUUID())
            .name("셔츠")
            .type(ClothesType.TOP)
            .build();
        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of(clothes));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine 동작 : 의상 추천
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather)))
            .thenReturn(List.of(clothes));

        RecommendationDto expected = new RecommendationDto(
            weatherId,
            userId,
            List.of(new ClothesDto(clothes.getId(), userId, "셔츠", "image.jpg", ClothesType.TOP, List.of()))
        );
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 추천 결과 검증
        assertThat(result.weatherId()).isEqualTo(weatherId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.clothes()).hasSize(1);
    }

    @Test
    void 추천_조회_실패_날씨_정보_없음() {
        // given: 날씨 정보 없음
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.empty());

        // when & then: 예외 발생
        assertThatThrownBy(() -> recommendationService.getRecommendation(userId, weatherId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("날씨 정보를 찾을 수 없습니다");
    }

    @Test
    void 추천_알고리즘_OUTER_두께_규칙() {
        // given: 추운 날씨, 두꺼운 OUTER 존재, 사용자 프로필 존재
        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(5.0)
            .maxC(7.0)
            .minC(3.0)
            .asWord(WindStrength.MODERATE)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .speedMs(3.0)
            .build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        ClothesAttribute thicknessAttr = ClothesAttribute.create(null, null, "HEAVY");
        Clothes clothes = Clothes.builder()
            .id(UUID.randomUUID())
            .name("패딩")
            .type(ClothesType.OUTER)
            .attributes(List.of(thicknessAttr))
            .build();
        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of(clothes));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine: 조건 충족 -> 추천 반환
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather)))
            .thenReturn(List.of(clothes));

        // Mapper Mock
        ClothesDto clothesDto = new ClothesDto(
            clothes.getId(),
            userId,
            "패딩",
            "image.jpg",
            ClothesType.OUTER,
            List.of()
        );
        RecommendationDto expected = new RecommendationDto(weatherId, userId, List.of(clothesDto));
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 추천 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.clothes()).hasSize(1);
    }

    @Test
    void 추천_실패_사용자_의상_없음() {
        // given: 날씨 정보는 존재, 사용자의 의상은 없음
        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(20.0)
            .maxC(25.0)
            .minC(18.0)
            .asWord(WindStrength.WEAK)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .speedMs(2.0)
            .build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of()); // 의상 없음

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine: 의상이 없으므로 추천 결과도 빈 리스트
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather)))
            .thenReturn(List.of());

        when(recommendationMapper.toDto(any(Recommendation.class)))
            .thenReturn(new RecommendationDto(weatherId, userId, List.of()));

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 추천 실패 → 추천 목록이 빈 리스트
        assertThat(result.clothes()).isEmpty();
    }

    @Test
    void 체감온도_기반_계절_분기_겨울() {
        // given: 날씨와 사용자 정보, 의상
        Weather weather = Weather.builder()
            .id(weatherId)
            .maxC(1.0)
            .minC(-2.0)
            .speedMs(3.0)
            .asWord(WindStrength.MODERATE)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        Clothes thickOuter = Clothes.builder()
            .id(UUID.randomUUID())
            .name("두꺼운 패딩")
            .type(ClothesType.OUTER)
            .attributes(List.of(ClothesAttribute.create(null, null, "HEAVY")))
            .build();
        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of(thickOuter));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(recommendationEngine.recommend(eq(List.of(thickOuter)), anyDouble(), eq(weather)))
            .thenReturn(List.of(thickOuter));

        RecommendationDto expected = new RecommendationDto(weatherId, userId, List.of(
            new ClothesDto(thickOuter.getId(), userId, "두꺼운 패딩", "image.jpg", ClothesType.OUTER, List.of())
        ));
        when(recommendationMapper.toDto(any())).thenReturn(expected);

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 결과 검증
        assertThat(result.clothes()).extracting("name").contains("두꺼운 패딩");
    }


    @Test
    void 체감온도_기반_계절_분기_봄() {
        // given: 날씨와 사용자 정보, 의상
        Weather weather = Weather.builder()
            .id(weatherId)
            .maxC(18.0)
            .minC(12.0)
            .speedMs(2.0)
            .asWord(WindStrength.WEAK)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        Clothes lightTop = Clothes.builder()
            .id(UUID.randomUUID())
            .name("얇은 셔츠")
            .type(ClothesType.TOP)
            .attributes(List.of(ClothesAttribute.create(null, null, "LIGHT")))
            .build();
        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of(lightTop));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(recommendationEngine.recommend(eq(List.of(lightTop)), anyDouble(), eq(weather)))
            .thenReturn(List.of(lightTop));

        RecommendationDto expected = new RecommendationDto(weatherId, userId, List.of(
            new ClothesDto(lightTop.getId(), userId, "얇은 셔츠", "image.jpg", ClothesType.TOP, List.of())
        ));
        when(recommendationMapper.toDto(any())).thenReturn(expected);

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 결과 검증
        assertThat(result.clothes()).extracting("name").contains("얇은 셔츠");
    }

    @Test
    void 세부_온도_카테고리_판별_봄_LOW_HIGH() {
        // given: 봄 구간에 해당하는 체감 온도
        double lowTemp = 15.0;   // 봄 LOW
        double highTemp = 22.0;  // 봄 HIGH
        Season season = Season.SPRING;

        // when: 세부 온도 카테고리 분류
        TemperatureCategory lowCategory = WeatherUtils.classifyTemperatureCategory(season, lowTemp);
        TemperatureCategory highCategory = WeatherUtils.classifyTemperatureCategory(season, highTemp);

        // then: 봄 LOW/HIGH 정상 분류
        assertThat(lowCategory).isEqualTo(TemperatureCategory.LOW);
        assertThat(highCategory).isEqualTo(TemperatureCategory.HIGH);
    }

    @Test
    void 세부_온도_카테고리_판별_여름_LOW_HIGH() {
        // given: 여름 구간 체감 온도
        double lowTemp = 25.0;   // 여름 LOW
        double highTemp = 30.0;  // 여름 HIGH
        Season season = Season.SUMMER;

        // when: 세부 온도 카테고리 분류
        TemperatureCategory lowCategory = WeatherUtils.classifyTemperatureCategory(season, lowTemp);
        TemperatureCategory highCategory = WeatherUtils.classifyTemperatureCategory(season, highTemp);

        // then: 여름 LOW/HIGH 정상 분류
        assertThat(lowCategory).isEqualTo(TemperatureCategory.LOW);
        assertThat(highCategory).isEqualTo(TemperatureCategory.HIGH);
    }

    @Test
    void 세부_온도_카테고리_판별_가을_LOW_HIGH() {
        // given: 가을 구간 체감 온도
        double lowTemp = 10.0;   // 가을 LOW
        double highTemp = 14.0;  // 가을 HIGH
        Season season = Season.FALL;

        // when: 세부 온도 카테고리 분류
        TemperatureCategory lowCategory = WeatherUtils.classifyTemperatureCategory(season, lowTemp);
        TemperatureCategory highCategory = WeatherUtils.classifyTemperatureCategory(season, highTemp);

        // then: 가을 LOW/HIGH 정상 분류
        assertThat(lowCategory).isEqualTo(TemperatureCategory.LOW);
        assertThat(highCategory).isEqualTo(TemperatureCategory.HIGH);
    }

    @Test
    void 세부_온도_카테고리_판별_겨울_LOW_HIGH() {
        // given: 겨울 구간 체감 온도
        double lowTemp = -5.0;  // 겨울 LOW
        double highTemp = 7.0;  // 겨울 HIGH
        Season season = Season.WINTER;

        // when: 세부 온도 카테고리 분류
        TemperatureCategory lowCategory = WeatherUtils.classifyTemperatureCategory(season, lowTemp);
        TemperatureCategory highCategory = WeatherUtils.classifyTemperatureCategory(season, highTemp);

        // then: 겨울 LOW/HIGH 정상 분류
        assertThat(lowCategory).isEqualTo(TemperatureCategory.LOW);
        assertThat(highCategory).isEqualTo(TemperatureCategory.HIGH);
    }

    @Test
    void 풍속_강하면_체감온도_낮아짐() {
        // given: 동일한 최대/최소 기온과 민감도
        double maxTemp = 10.0;
        double minTemp = 10.0;
        double windFactor = 1.0;
        int sensitivity = 2; // 보통 민감도( 추위 조금 )

        // when: 풍속 0과 강한 풍속에서 체감온도 계산
        double perceivedNoWind = WeatherUtils.calculatePerceivedTemperature(
            maxTemp, minTemp, 0.0, windFactor, sensitivity
        );
        double perceivedStrongWind = WeatherUtils.calculatePerceivedTemperature(
            maxTemp, minTemp, 8.0, windFactor, sensitivity
        );

        // then: 풍속 강하면 체감온도가 낮아짐
        assertThat(perceivedStrongWind)
            .isLessThan(perceivedNoWind)
            .isLessThan((maxTemp + minTemp) / 2.0);
    }

    @Test
    void 날씨_상태_필터링_구름많고_비_얇은옷_제외() {
        // given: 구름 많고 비오는 날씨, 얇은 티셔츠 존재
        Weather weather = Weather.builder()
            .id(weatherId).maxC(18.0).minC(16.0)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.RAIN)
            .speedMs(2.0).build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        Clothes thinTop = Clothes.builder().id(UUID.randomUUID())
            .name("얇은 티셔츠").type(ClothesType.TOP)
            .attributes(List.of(ClothesAttribute.create(null, null, "LIGHT"))).build();
        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of(thinTop));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine은 필터링 결과: 빈 리스트 반환
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather)))
            .thenReturn(List.of());

        when(recommendationMapper.toDto(any())).thenReturn(new RecommendationDto(weatherId, userId, List.of()));

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 얇은 티셔츠 제외 -> 추천 리스트 빔
        assertThat(result.clothes()).isEmpty();
    }

    @Test
    void 일교차_클때_OUTER_추천() {
        // given: 큰 일교차, OUTER 존재
        Weather weather = Weather.builder()
            .id(weatherId).maxC(25.0).minC(10.0).speedMs(1.0).build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        Clothes cardigan = Clothes.builder().id(UUID.randomUUID())
            .name("가디건").type(ClothesType.OUTER)
            .attributes(List.of(ClothesAttribute.create(null, null, "LIGHT"))).build();
        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of(cardigan));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather)))
            .thenReturn(List.of(cardigan));

        when(recommendationMapper.toDto(any())).thenReturn(new RecommendationDto(weatherId, userId,
            List.of(new ClothesDto(cardigan.getId(), userId, "가디건", "image.jpg", ClothesType.OUTER, List.of()))));

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 가디건 추천
        assertThat(result.clothes()).extracting("name").contains("가디건");
    }

    @Test
    void 조건불만족_필터링제외() {
        // given: 조건 불만족 날씨, 두꺼운 코트 존재
        Weather weather = Weather.builder().id(weatherId).maxC(30.0).minC(29.0).speedMs(0.5).build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        Clothes heavyCoat = Clothes.builder().id(UUID.randomUUID())
            .name("두꺼운 코트").type(ClothesType.OUTER)
            .attributes(List.of(ClothesAttribute.create(null, null, "HEAVY"))).build();
        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of(heavyCoat));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine 필터링 결과 : 조건 불만족 -> 빈 리스트 반환
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather)))
            .thenReturn(List.of());
        when(recommendationMapper.toDto(any())).thenReturn(new RecommendationDto(weatherId, userId, List.of()));

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 추천 리스트 비어있음
        assertThat(result.clothes()).isEmpty();
    }

    @Test
    void 두께_속성_없어도_추천동작() {
        // given: 두께 속성 없는 OUTER 존재
        Weather weather = Weather.builder().id(weatherId).maxC(15.0).minC(13.0).speedMs(2.0).build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        Clothes outer = Clothes.builder().id(UUID.randomUUID())
            .name("자켓").type(ClothesType.OUTER).attributes(List.of()).build(); // 속성 없음
        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of(outer));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather)))
            .thenReturn(List.of(outer));

        when(recommendationMapper.toDto(any())).thenReturn(new RecommendationDto(weatherId, userId,
            List.of(new ClothesDto(outer.getId(), userId, "자켓", "image.jpg", ClothesType.OUTER, List.of()))));

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 자켓 추천
        assertThat(result.clothes()).isNotEmpty();
    }

    @Test
    void 두께_속성값_없으면_다른_두께_추천() {
        // given: 두께 값 없는 OUTER와 HEAVY OUTER 존재
        Weather weather = Weather.builder().id(weatherId).maxC(5.0).minC(0.0).speedMs(3.0).build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        Clothes noValueOuter = Clothes.builder().id(UUID.randomUUID())
            .name("미상 두께 외투").type(ClothesType.OUTER)
            .attributes(List.of(ClothesAttribute.create(null, null, null))).build(); // 값 없음
        Clothes heavyOuter = Clothes.builder().id(UUID.randomUUID())
            .name("두꺼운 패딩").type(ClothesType.OUTER)
            .attributes(List.of(ClothesAttribute.create(null, null, "HEAVY"))).build();

        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of(noValueOuter, heavyOuter));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine 결과: 미상 두께 외투( noValueOuter ) 제외, 두꺼운 패팅( heavyOuter ) 추천
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather)))
            .thenReturn(List.of(heavyOuter));
        when(recommendationMapper.toDto(any())).thenReturn(new RecommendationDto(weatherId, userId,
            List.of(new ClothesDto(heavyOuter.getId(), userId, "두꺼운 패딩", "image.jpg", ClothesType.OUTER, List.of()))));

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 두꺼운 패딩 추천
        assertThat(result.clothes()).extracting("name").contains("두꺼운 패딩");
    }
}