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
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.entity.Recommendation;
import com.sprint.otboo.recommendation.mapper.RecommendationMapper;
import com.sprint.otboo.recommendation.repository.RecommendationRepository;
import com.sprint.otboo.recommendation.util.RecommendationEngine;
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
    @DisplayName("추천 조회 성공 - 기본 타입 규칙")
    void getRecommendation_success() {
        // given
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

        // RecommendationEngine 동작 Mock
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather)))
            .thenReturn(List.of(clothes));

        RecommendationDto expected = new RecommendationDto(
            weatherId,
            userId,
            List.of(new ClothesDto(clothes.getId(), userId, "셔츠", "image.jpg", ClothesType.TOP, List.of()))
        );
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then
        assertThat(result.weatherId()).isEqualTo(weatherId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.clothes()).hasSize(1);
    }

    @Test
    @DisplayName("추천 조회 실패 - 날씨 정보 없음")
    void getRecommendation_weatherNotFound() {
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getRecommendation(userId, weatherId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("날씨 정보를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("추천 알고리즘 - BOTTOM, 비+코튼 소재 제외")
    void bottomRecommendationRainCotton() {
        // given
        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(20.0)
            .maxC(22.0)
            .minC(18.0)
            .asWord(WindStrength.WEAK)
            .skyStatus(SkyStatus.CLOUDY)
            .type(PrecipitationType.RAIN)
            .speedMs(2.0)
            .build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        ClothesAttribute materialAttr = ClothesAttribute.create(null, null, "COTTON");
        Clothes clothes = Clothes.builder()
            .id(UUID.randomUUID())
            .name("청바지")
            .type(ClothesType.BOTTOM)
            .attributes(List.of(materialAttr))
            .build();
        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of(clothes));

        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine Mock: 제외 규칙 적용 -> 빈 리스트
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather)))
            .thenReturn(List.of());

        // Mapper Mock
        RecommendationDto expected = new RecommendationDto(weatherId, userId, List.of());
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.clothes()).isEmpty(); // BOTTOM + 비 + COTTON 제외
    }

    @Test
    @DisplayName("추천 알고리즘 - TOP, 소재 오류는 제외 처리")
    void topRecommendation_invalidMaterial() {
        Weather weather = Weather.builder()
            .id(weatherId)
            .currentC(25.0)
            .maxC(27.0)
            .minC(23.0)
            .asWord(WindStrength.WEAK)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .speedMs(1.0)
            .build();
        when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

        ClothesAttribute materialAttr = ClothesAttribute.create(null, null, "UNKNOWN_MATERIAL");
        Clothes clothes = Clothes.builder()
            .id(UUID.randomUUID())
            .name("셔츠")
            .type(ClothesType.TOP)
            .attributes(List.of(materialAttr))
            .build();
        when(clothesRepository.findByUser_Id(userId)).thenReturn(List.of(clothes));

        UserProfile profile = UserProfile.builder()
            .userId(UUID.randomUUID())
            .user(User.builder().id(userId).build())
            .temperatureSensitivity(0)
            .build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Engine 결과: UNKNOWN_MATERIAL은 제외 -> 빈 리스트
        when(recommendationEngine.recommend(anyList(), anyDouble(), eq(weather)))
            .thenReturn(List.of());

        // Mapper 결과 Mock
        RecommendationDto expected = new RecommendationDto(weatherId, userId, List.of());
        when(recommendationMapper.toDto(any(Recommendation.class))).thenReturn(expected);

        // when
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.clothes()).isEmpty();
    }

    @Test
    @DisplayName("추천 알고리즘 - OUTER, 두께 규칙")
    void outerRecommendation_thickness() {
        // given
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

        // Engine Mock: 조건 충족 -> 반환
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

        // when
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.clothes()).hasSize(1);
    }
}