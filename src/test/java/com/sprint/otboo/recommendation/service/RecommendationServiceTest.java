package com.sprint.otboo.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.Season;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.entity.Recommendation;
import com.sprint.otboo.recommendation.entity.RecommendationClothes;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

        // weatherRepository stub
        when(weatherRepository.findByIdWithLocation(eq(weatherId)))
            .thenReturn(Optional.of(weather));

        Clothes clothes = Clothes.builder()
            .id(UUID.randomUUID())
            .name("셔츠")
            .type(ClothesType.TOP)
            .build();

        // clothesRepository stub
        when(clothesRepository.findByUserIdWithAttributes(any(UUID.class)))
            .thenReturn(List.of(clothes));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();

        // userProfileRepository stub
        when(userProfileRepository.findByUserId(any(UUID.class)))
            .thenReturn(Optional.of(profile));

        // recommendationEngine stub
        when(recommendationEngine.recommend(anyList(), anyDouble(), any(Weather.class), anyBoolean()))
            .thenReturn(List.of(clothes));

        RecommendationDto expected = new RecommendationDto(
            weatherId,
            userId,
            List.of(new ClothesDto(clothes.getId(), userId, "셔츠", "image.jpg", ClothesType.TOP, List.of()))
        );

        // recommendationMapper stub
        when(recommendationMapper.toDto(any(Recommendation.class)))
            .thenReturn(expected);

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
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.empty());

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
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        ClothesAttribute thicknessAttr = ClothesAttribute.create(null, null, "HEAVY");
        Clothes clothes = Clothes.builder()
            .id(UUID.randomUUID())
            .name("패딩")
            .type(ClothesType.OUTER)
            .attributes(List.of(thicknessAttr))
            .build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(clothes));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine: 조건 충족 -> 추천 반환
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
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
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of()); // 의상 없음

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine: 의상이 없으므로 추천 결과도 빈 리스트
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
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
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes thickOuter = Clothes.builder()
            .id(UUID.randomUUID())
            .name("두꺼운 패딩")
            .type(ClothesType.OUTER)
            .attributes(List.of(ClothesAttribute.create(null, null, "HEAVY")))
            .build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(thickOuter));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(recommendationEngine.recommend(eq(List.of(thickOuter)), anyDouble(), eq(weather), anyBoolean()))
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
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes lightTop = Clothes.builder()
            .id(UUID.randomUUID())
            .name("얇은 셔츠")
            .type(ClothesType.TOP)
            .attributes(List.of(ClothesAttribute.create(null, null, "LIGHT")))
            .build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(lightTop));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(recommendationEngine.recommend(eq(List.of(lightTop)), anyDouble(), eq(weather), anyBoolean()))
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
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes thinTop = Clothes.builder().id(UUID.randomUUID())
            .name("얇은 티셔츠").type(ClothesType.TOP)
            .attributes(List.of(ClothesAttribute.create(null, null, "LIGHT"))).build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(thinTop));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine은 필터링 결과: 빈 리스트 반환
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
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
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes cardigan = Clothes.builder().id(UUID.randomUUID())
            .name("가디건").type(ClothesType.OUTER)
            .attributes(List.of(ClothesAttribute.create(null, null, "LIGHT"))).build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(cardigan));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
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
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes heavyCoat = Clothes.builder().id(UUID.randomUUID())
            .name("두꺼운 코트").type(ClothesType.OUTER)
            .attributes(List.of(ClothesAttribute.create(null, null, "HEAVY"))).build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(heavyCoat));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine 필터링 결과 : 조건 불만족 -> 빈 리스트 반환
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
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
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes outer = Clothes.builder().id(UUID.randomUUID())
            .name("자켓").type(ClothesType.OUTER).attributes(List.of()).build(); // 속성 없음
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(outer));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
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
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes noValueOuter = Clothes.builder().id(UUID.randomUUID())
            .name("미상 두께 외투").type(ClothesType.OUTER)
            .attributes(List.of(ClothesAttribute.create(null, null, null))).build(); // 값 없음
        Clothes heavyOuter = Clothes.builder().id(UUID.randomUUID())
            .name("두꺼운 패딩").type(ClothesType.OUTER)
            .attributes(List.of(ClothesAttribute.create(null, null, "HEAVY"))).build();

        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(noValueOuter, heavyOuter));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine 결과: 미상 두께 외투( noValueOuter ) 제외, 두꺼운 패팅( heavyOuter ) 추천
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
            .thenReturn(List.of(heavyOuter));
        when(recommendationMapper.toDto(any())).thenReturn(new RecommendationDto(weatherId, userId,
            List.of(new ClothesDto(heavyOuter.getId(), userId, "두꺼운 패딩", "image.jpg", ClothesType.OUTER, List.of()))));

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 두꺼운 패딩 추천
        assertThat(result.clothes()).extracting("name").contains("두꺼운 패딩");
    }

    @Test
    void 최근_추천_제외_및_Dress_제외_로직_테스트() {
        // given: 날씨 정보, 사용자 의상 2개(Dress + Top), 사용자 프로필, 최근 추천 기록 포함
        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(22.0)
            .maxC(24.0)
            .minC(20.0)
            .asWord(WindStrength.WEAK)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .speedMs(1.5)
            .build();
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes dress = Clothes.builder()
            .id(UUID.randomUUID())
            .name("원피스")
            .type(ClothesType.DRESS)
            .build();
        Clothes top = Clothes.builder()
            .id(UUID.randomUUID())
            .name("셔츠")
            .type(ClothesType.TOP)
            .build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(dress, top));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Recommendation recentRecommendation = Recommendation.builder()
            .user(User.builder().id(userId).build())
            .weather(weather)
            .build();
        recentRecommendation.addRecommendationClothes(
            RecommendationClothes.builder().clothes(dress).recommendation(recentRecommendation).build()
        );
        when(recommendationRepository.findByUser_IdAndCreatedAtAfter(eq(userId), any()))
            .thenReturn(List.of(recentRecommendation));

        // Engine 동작: 필터링 후 Dress 제외 → Top 추천
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
            .thenReturn(List.of(top));

        RecommendationDto expected = new RecommendationDto(
            weatherId,
            userId,
            List.of(new ClothesDto(top.getId(), userId, "셔츠", "image.jpg", ClothesType.TOP, List.of()))
        );
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: Dress는 제외되고 Top만 추천
        assertThat(result.clothes()).hasSize(1);
        assertThat(result.clothes().get(0).type()).isEqualTo(ClothesType.TOP);
    }

    @Test
    void 최근_10분내_추천된_의상은_제외된다() {
        // given: 날씨, 사용자, 의상 2벌, 10분 내 추천 이력 존재
        UUID weatherId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(20.0)
            .maxC(22.0)
            .minC(18.0)
            .asWord(WindStrength.WEAK)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .speedMs(1.5)
            .build();
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes top = Clothes.builder()
            .id(UUID.randomUUID())
            .name("셔츠")
            .type(ClothesType.TOP)
            .build();

        Clothes bottom = Clothes.builder()
            .id(UUID.randomUUID())
            .name("바지")
            .type(ClothesType.BOTTOM)
            .build();

        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(top, bottom));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // 최근 10분 내 추천된 옷 = top
        Recommendation recent = Recommendation.builder()
            .user(User.builder().id(userId).build())
            .weather(weather)
            .createdAt(Instant.now().minus(5, ChronoUnit.MINUTES)) // Instant 기준 10분 내
            .build();
        recent.addRecommendationClothes(
            RecommendationClothes.builder().clothes(top).recommendation(recent).build()
        );

        when(recommendationRepository.findByUser_IdAndCreatedAtAfter(eq(userId), any()))
            .thenReturn(List.of(recent));

        // Engine은 제외된 top을 제거하고 bottom만 추천하도록 설정
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
            .thenAnswer(invocation -> {
                List<Clothes> clothes = invocation.getArgument(0);
                return clothes.stream()
                    .filter(c -> c.getType() == ClothesType.BOTTOM)
                    .toList();
            });

        RecommendationDto expected = new RecommendationDto(
            weatherId,
            userId,
            List.of(new ClothesDto(bottom.getId(), userId, "바지", "image.jpg", ClothesType.BOTTOM, List.of()))
        );
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when: 추천 요청 실행
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 10분 내 추천된 top은 제외되고 bottom만 추천
        assertThat(result.clothes()).hasSize(1);
        assertThat(result.clothes().get(0).name()).isEqualTo("바지");
    }

    @Test
    void 십분지난_추천은_재추천된다() {
        // given: 날씨, 사용자, 의상 1벌, 추천 이력(10분 이상 지난 기록)
        UUID weatherId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(18.0)
            .maxC(20.0)
            .minC(16.0)
            .asWord(WindStrength.WEAK)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .speedMs(2.0)
            .build();
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes top = Clothes.builder()
            .id(UUID.randomUUID())
            .name("맨투맨")
            .type(ClothesType.TOP)
            .build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(top));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // 10분이 지난 추천 이력
        Recommendation oldRec = Recommendation.builder()
            .user(User.builder().id(userId).build())
            .weather(weather)
            .createdAt(Instant.now().minus(15, ChronoUnit.MINUTES)) // 10분 초과
            .build();
        oldRec.addRecommendationClothes(
            RecommendationClothes.builder().clothes(top).recommendation(oldRec).build()
        );

        when(recommendationRepository.findByUser_IdAndCreatedAtAfter(eq(userId), any()))
            .thenReturn(List.of()); // 10분 초과 → 조회 시점에서 비포함

        // Engine은 top 추천 반환
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
            .thenReturn(List.of(top));

        RecommendationDto expected = new RecommendationDto(
            weatherId,
            userId,
            List.of(new ClothesDto(top.getId(), userId, "맨투맨", "image.jpg", ClothesType.TOP, List.of()))
        );
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 10분 지난 추천은 필터에서 제외되지 않음 → 다시 추천됨
        assertThat(result.clothes()).hasSize(1);
        assertThat(result.clothes().get(0).name()).isEqualTo("맨투맨");
    }

    @Test
    void 첫번째추천엔진결과없을때_Fallback으로_전체의상재추천() {
        // given: 날씨, 사용자, 의상 2벌, 추천엔진 1차 실패 → Fallback 내부 재보충
        UUID weatherId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(19.0)
            .maxC(21.0)
            .minC(17.0)
            .asWord(WindStrength.MODERATE)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.NONE)
            .speedMs(2.5)
            .build();
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes top = Clothes.builder()
            .id(UUID.randomUUID())
            .name("맨투맨")
            .type(ClothesType.TOP)
            .build();
        Clothes bottom = Clothes.builder()
            .id(UUID.randomUUID())
            .name("청바지")
            .type(ClothesType.BOTTOM)
            .build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(top, bottom));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // 최근 추천 없음
        when(recommendationRepository.findByUser_IdAndCreatedAtAfter(eq(userId), any()))
            .thenReturn(List.of());

        // recommend() 한 번만 호출 → 빈 결과면 서비스 내부 Fallback 보충
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
            .thenReturn(List.of(bottom));

        RecommendationDto expected = new RecommendationDto(
            weatherId,
            userId,
            List.of(new ClothesDto(bottom.getId(), userId, "청바지", "image.jpg", ClothesType.BOTTOM, List.of()))
        );
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when: 추천 요청 실행
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then
        // 1) 첫 번째 recommend 호출 결과가 빈 리스트였을 경우
        // 2) 서비스 내부 Fallback 로직으로 보충되어 최종적으로 bottom 추천됨
        assertThat(result.clothes()).hasSize(1);
        assertThat(result.clothes().get(0).name()).isEqualTo("청바지");

        verify(recommendationEngine, times(1))
            .recommend(anyList(), anyDouble(), eq(weather), anyBoolean());
    }

    @Test
    void FallBack_적용되는_타입과_적용되지_않는_타입_테스트() {
        // given: 날씨, 사용자 의상 2벌 (Top, Dress), 사용자 프로필, 최근 추천에 Top 포함 (10분 내)
        UUID weatherId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(22.0)
            .maxC(24.0)
            .minC(20.0)
            .asWord(WindStrength.WEAK)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .speedMs(1.5)
            .build();
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes top = Clothes.builder().id(UUID.randomUUID()).name("셔츠").type(ClothesType.TOP).build();
        Clothes dress = Clothes.builder().id(UUID.randomUUID()).name("원피스").type(ClothesType.DRESS).build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(top, dress));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // 최근 추천에 top 포함 (10분 내)
        Recommendation recent = Recommendation.builder()
            .user(User.builder().id(userId).build())
            .weather(weather)
            .createdAt(Instant.now().minus(5, ChronoUnit.MINUTES))
            .build();
        recent.addRecommendationClothes(RecommendationClothes.builder().clothes(top).recommendation(recent).build());
        when(recommendationRepository.findByUser_IdAndCreatedAtAfter(eq(userId), any()))
            .thenReturn(List.of(recent));

        // Engine: filtered clothes empty → fallback으로 Dress 반환
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
            .thenAnswer(invocation -> {
                List<Clothes> clothes = invocation.getArgument(0);
                boolean excludeDress = invocation.getArgument(3);
                // filtered clothes가 empty이면 fallback으로 dress 반환
                if (clothes.isEmpty() && excludeDress) {
                    return List.of(dress);
                }
                return clothes.stream()
                    .filter(c -> !excludeDress || c.getType() != ClothesType.DRESS)
                    .toList();
            });

        RecommendationDto expected = new RecommendationDto(
            weatherId,
            userId,
            List.of(new ClothesDto(dress.getId(), userId, "원피스", "image.jpg", ClothesType.DRESS, List.of()))
        );
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: fallback 적용으로 Dress 추천, Top 제외
        assertThat(result.clothes()).hasSize(1);
        assertThat(result.clothes().get(0).type()).isEqualTo(ClothesType.DRESS);
    }

    @Test
    void FallBack_적용후_Dress와_TopBottom_상호배타적_유지() {
        // given: 최근 추천은 Dress, Fallback 적용 후 새 추천은 Top/Bottom으로 대체됨
        UUID weatherId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(24.0)
            .maxC(26.0)
            .minC(22.0)
            .asWord(WindStrength.MODERATE)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .speedMs(2.5)
            .build();
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes dress = Clothes.builder().id(UUID.randomUUID()).name("원피스").type(ClothesType.DRESS).build();
        Clothes top = Clothes.builder().id(UUID.randomUUID()).name("반팔 티셔츠").type(ClothesType.TOP).build();
        Clothes bottom = Clothes.builder().id(UUID.randomUUID()).name("린넨 팬츠").type(ClothesType.BOTTOM).build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(dress, top, bottom));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Recommendation recent = Recommendation.builder()
            .user(User.builder().id(userId).build())
            .weather(weather)
            .createdAt(Instant.now().minus(5, ChronoUnit.MINUTES))
            .build();
        recent.addRecommendationClothes(
            RecommendationClothes.builder().clothes(dress).recommendation(recent).build()
        );
        when(recommendationRepository.findByUser_IdAndCreatedAtAfter(eq(userId), any()))
            .thenReturn(List.of(recent));

        when(recommendationEngine.recommend(anyList(), anyDouble(), any(Weather.class), anyBoolean()))
            .thenReturn(List.of(top, bottom));

        RecommendationDto expected = new RecommendationDto(
            weatherId,
            userId,
            List.of(
                new ClothesDto(top.getId(), userId, "반팔 티셔츠", "image.jpg", ClothesType.TOP, List.of()),
                new ClothesDto(bottom.getId(), userId, "린넨 팬츠", "image.jpg", ClothesType.BOTTOM, List.of())
            )
        );
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when: 추천 요청 실행
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: Dress는 제외되고 Top & Bottom 조합만 남음
        assertThat(result.clothes())
            .extracting(ClothesDto::type)
            .containsExactlyInAnyOrder(ClothesType.TOP, ClothesType.BOTTOM);
    }

    @Test
    void 타입별_FallBack_적용_및_기존추천유지() {
        // given: 사용자, 날씨, 의상 및 이전 추천 기록
        UUID weatherId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(22.0)
            .maxC(24.0)
            .minC(20.0)
            .asWord(WindStrength.WEAK)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .speedMs(1.5)
            .build();
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes top = Clothes.builder().id(UUID.randomUUID()).name("셔츠").type(ClothesType.TOP).build();
        Clothes bottom = Clothes.builder().id(UUID.randomUUID()).name("바지").type(ClothesType.BOTTOM).build();
        // HAT은 사용자의 의상 목록에 없음
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(top, bottom));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // 이전 추천: Top, Bottom, HAT (10분 내)
        Recommendation recent = Recommendation.builder()
            .user(User.builder().id(userId).build())
            .weather(weather)
            .createdAt(Instant.now().minus(5, ChronoUnit.MINUTES))
            .build();
        recent.addRecommendationClothes(RecommendationClothes.builder().clothes(top).recommendation(recent).build());
        recent.addRecommendationClothes(RecommendationClothes.builder().clothes(bottom).recommendation(recent).build());

        // FallBack으로 추천될 HAT
        Clothes fallbackHat = Clothes.builder().id(UUID.randomUUID()).name("모자").type(ClothesType.HAT).build();

        when(recommendationRepository.findByUser_IdAndCreatedAtAfter(eq(userId), any()))
            .thenReturn(List.of(recent));

        // Engine 스텁: filtered clothes에 HAT이 없으면 FallBack으로 HAT 반환
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
            .thenAnswer(invocation -> {
                List<Clothes> clothes = invocation.getArgument(0);
                boolean excludeDress = invocation.getArgument(3);
                if (clothes.stream().noneMatch(c -> c.getType() == ClothesType.HAT)) {
                    return List.of(fallbackHat);
                }
                return clothes;
            });

        RecommendationDto expected = new RecommendationDto(
            weatherId,
            userId,
            List.of(
                new ClothesDto(top.getId(), userId, "셔츠", "image.jpg", ClothesType.TOP, List.of()),
                new ClothesDto(bottom.getId(), userId, "바지", "image.jpg", ClothesType.BOTTOM, List.of()),
                new ClothesDto(fallbackHat.getId(), userId, "모자", "image.jpg", ClothesType.HAT, List.of())
            )
        );
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when: 추천 서비스 호출
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: Top, Bottom은 기존 추천 유지, HAT은 FallBack으로 추천
        assertThat(result.clothes())
            .extracting(ClothesDto::type)
            .containsExactlyInAnyOrder(ClothesType.TOP, ClothesType.BOTTOM, ClothesType.HAT);
    }

    @Test
    void fallback_적용_확률적_대체_의상_추가() {
        // given: 특정 타입(TOP)만 존재하고, 다른 타입은 누락된 사용자 의상
        UUID userId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();

        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(18.0)
            .speedMs(2.0)
            .build();
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes top = Clothes.builder()
            .id(UUID.randomUUID())
            .name("셔츠")
            .type(ClothesType.TOP)
            .build();
        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(top));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Fallback 후보 (Dress)
        Clothes fallbackDress = Clothes.builder()
            .id(UUID.randomUUID())
            .name("드레스")
            .type(ClothesType.DRESS)
            .build();

        // 모든 타입 호출 허용, DRESS만 Optional.of 반환, 나머지는 Optional.empty
        when(clothesRepository.findFirstByType(any())).thenAnswer(invocation -> {
            ClothesType type = invocation.getArgument(0);
            if (type == ClothesType.DRESS) return Optional.of(fallbackDress);
            return Optional.empty();
        });

        // 추천 엔진 결과: TOP만 존재
        when(recommendationEngine.recommend(anyList(), anyDouble(), any(), anyBoolean()))
            .thenReturn(List.of(top));

        RecommendationDto expected = new RecommendationDto(
            weatherId,
            userId,
            List.of(new ClothesDto(top.getId(), userId, "셔츠", "image.jpg", ClothesType.TOP, List.of()))
        );
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when: 추천 요청
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: Fallback이 확률적으로 포함될 수도, 아닐 수도 있음
        assertThat(result.clothes()).isNotEmpty();
        assertThat(result.clothes())
            .extracting(ClothesDto::type)
            .contains(ClothesType.TOP); // TOP는 항상 존재

        // verify: fallback 후보 조회 최소 1회 이상
        verify(clothesRepository, atLeastOnce()).findFirstByType(any());
    }

    @Test
    void mutualExclusion_Dress_과다시_Dress_제외() {
        // given: 날씨, 사용자, 의상 3벌, 최근 추천 2회 모두 Dress 포함
        UUID userId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();

        Weather weather = Weather.builder().id(weatherId).currentC(20.0).build();
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes dress = Clothes.builder().id(UUID.randomUUID()).type(ClothesType.DRESS).build();
        Clothes top = Clothes.builder().id(UUID.randomUUID()).type(ClothesType.TOP).build();
        Clothes bottom = Clothes.builder().id(UUID.randomUUID()).type(ClothesType.BOTTOM).build();

        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(dress, top, bottom));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // 최근 추천 2회 모두 Dress 포함
        Recommendation rec1 = Recommendation.builder()
            .createdAt(Instant.now().minus(5, ChronoUnit.MINUTES))
            .build();
        rec1.addRecommendationClothes(RecommendationClothes.builder().clothes(dress).recommendation(rec1).build());

        Recommendation rec2 = Recommendation.builder()
            .createdAt(Instant.now().minus(2, ChronoUnit.MINUTES))
            .build();
        rec2.addRecommendationClothes(RecommendationClothes.builder().clothes(dress).recommendation(rec2).build());

        when(recommendationRepository.findByUser_IdAndCreatedAtAfter(eq(userId), any()))
            .thenReturn(List.of(rec1, rec2));

        // 이번 추천에서는 Dress, Top, Bottom 모두 추천될 예정
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
            .thenReturn(List.of(dress, top, bottom));

        RecommendationDto expectedDto = new RecommendationDto(weatherId, userId, List.of());
        when(recommendationMapper.toDto(any())).thenReturn(expectedDto);

        // when: 추천 요청 실행
        recommendationService.getRecommendation(userId, weatherId);

        // then: 최근 Dress 과다 추천으로 이번 추천에서는 Dress 제외
        verify(recommendationMapper).toDto(argThat(r ->
            r.getRecommendationClothes().stream()
                .noneMatch(rc -> rc.getClothes().getType() == ClothesType.DRESS)
        ));
    }

    @Test
    void mutualExclusion_TopBottom_과다시_TopBottom_제외() {
        // given: 날씨, 사용자, 의상 3벌, 최근 추천 2회 모두 Top & Bottom 포함
        UUID userId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();

        Weather weather = Weather.builder().id(weatherId).currentC(19.0).build();
        when(weatherRepository.findByIdWithLocation(weatherId)).thenReturn(Optional.of(weather));

        Clothes dress = Clothes.builder().id(UUID.randomUUID()).type(ClothesType.DRESS).build();
        Clothes top = Clothes.builder().id(UUID.randomUUID()).type(ClothesType.TOP).build();
        Clothes bottom = Clothes.builder().id(UUID.randomUUID()).type(ClothesType.BOTTOM).build();

        when(clothesRepository.findByUserIdWithAttributes(userId)).thenReturn(List.of(dress, top, bottom));

        UserProfile profile = UserProfile.builder().userId(userId).temperatureSensitivity(0).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // 최근 추천 2회 모두 Top & Bottom 포함
        Recommendation rec1 = Recommendation.builder()
            .createdAt(Instant.now().minus(5, ChronoUnit.MINUTES))
            .build();
        rec1.addRecommendationClothes(
            RecommendationClothes.builder().clothes(top).recommendation(rec1).build()
        );

        Recommendation rec2 = Recommendation.builder()
            .createdAt(Instant.now().minus(2, ChronoUnit.MINUTES))
            .build();
        rec2.addRecommendationClothes(
            RecommendationClothes.builder().clothes(bottom).recommendation(rec2).build()
        );

        when(recommendationRepository.findByUser_IdAndCreatedAtAfter(eq(userId), any()))
            .thenReturn(List.of(rec1, rec2));

        // 이번 추천에서는 Dress, Top, Bottom 모두 추천됨
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather), anyBoolean()))
            .thenReturn(List.of(dress, top, bottom));

        RecommendationDto expectedDto = new RecommendationDto(weatherId, userId, List.of());
        when(recommendationMapper.toDto(any())).thenReturn(expectedDto);

        // when: 추천 서비스 실행
        recommendationService.getRecommendation(userId, weatherId);

        // then: 최근 Top & Bottom 과다 추천으로 이번 추천에서는 Top과 Bottom 제외
        verify(recommendationMapper).toDto(argThat(r ->
            r.getRecommendationClothes().stream()
                .noneMatch(rc -> rc.getClothes().getType() == ClothesType.TOP
                    || rc.getClothes().getType() == ClothesType.BOTTOM)
        ));
    }
}