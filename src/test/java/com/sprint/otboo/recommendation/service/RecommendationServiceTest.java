package com.sprint.otboo.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.entity.Recommendation;
import com.sprint.otboo.recommendation.entity.RecommendationClothes;
import com.sprint.otboo.recommendation.exception.RecommendationNotFoundException;
import com.sprint.otboo.recommendation.mapper.RecommendationMapper;
import com.sprint.otboo.recommendation.repository.RecommendationRepository;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.util.Collections;
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

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @Test
    void 추천_조회_성공() {
        // given: 사용자와 날씨에 대한 추천 데이터 존재
        UUID userId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();

        User user = User.builder().id(userId).build();
        Weather weather = Weather.builder().id(weatherId).build();

        Clothes clothes = Clothes.builder()
            .id(UUID.randomUUID())
            .name("셔츠")
            .type(ClothesType.TOP)
            .build();

        Recommendation recommendation = Recommendation.builder()
            .user(user)
            .weather(weather)
            .build();

        RecommendationClothes rc = RecommendationClothes.builder()
            .clothes(clothes)
            .recommendation(recommendation)
            .build();
        recommendation.addRecommendationClothes(rc);

        ClothesDto clothesDto = new ClothesDto(
            clothes.getId(),
            userId,
            "셔츠",
            "image.jpg",
            ClothesType.TOP,
            List.of(new ClothesAttributeDto(UUID.randomUUID(), "화이트"))
        );

        RecommendationDto expected = new RecommendationDto(weatherId, userId, List.of(clothesDto));

        when(recommendationRepository.findByUser_IdAndWeather_Id(userId, weatherId))
            .thenReturn(Optional.of(recommendation));
        when(recommendationMapper.toDto(recommendation)).thenReturn(expected);

        // when: 추천 조회 요청
        RecommendationDto result = recommendationService.getRecommendation(userId, weatherId);

        // then: 기대한 추천 Dto 반환
        assertThat(result.weatherId()).isEqualTo(weatherId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.clothes()).hasSize(1);
    }

    @Test
    @DisplayName("추천 조회 실패 - 존재하지 않음")
    void getRecommendation_notFound() {
        // given: 사용자와 날씨에 대한 추천 데이터 없음
        UUID userId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();
        when(recommendationRepository.findByUser_IdAndWeather_Id(userId, weatherId))
            .thenReturn(Optional.empty());

        // when / then: 추천 조회 시 예외 발생
        assertThatThrownBy(() -> recommendationService.getRecommendation(userId, weatherId))
            .isInstanceOf(RecommendationNotFoundException.class);
    }
}