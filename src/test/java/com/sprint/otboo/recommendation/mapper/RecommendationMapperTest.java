package com.sprint.otboo.recommendation.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.entity.Recommendation;
import com.sprint.otboo.recommendation.entity.RecommendationClothes;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.weather.entity.Weather;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("RecommendationMapper 단위 테스트")
public class RecommendationMapperTest {

    @Autowired
    private RecommendationMapper recommendationMapper;

    private User testUser;
    private Clothes testClothes;
    private RecommendationClothes testRecommendationClothes;
    private Recommendation testRecommendation;

    @BeforeEach
    void setUp() {
        // given: 테스트용 사용자, 의상, 추천 관계, Recommendation 엔티티 생성
        testUser = User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .build();

        testClothes = Clothes.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .name("Top 1")
            .imageUrl("top1.jpg")
            .type(ClothesType.TOP)
            .build();

        testRecommendationClothes = RecommendationClothes.builder()
            .id(UUID.randomUUID())
            .clothes(testClothes)
            .build();

        testRecommendation = Recommendation.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .weather(Weather.builder().id(UUID.randomUUID()).build())
            .recommendationClothes(List.of(testRecommendationClothes))
            .build();
    }

    @Test
    void Recommendation을_RecommendationDto로_변환한다() {
        // when: Recommendation → RecommendationDto 변환
        RecommendationDto dto = recommendationMapper.toDto(testRecommendation);

        // then: 변환 결과 검증
        assertThat(dto).isNotNull();
        assertThat(dto.weatherId()).isEqualTo(testRecommendation.getWeather().getId());
        assertThat(dto.userId()).isEqualTo(testUser.getId());
        assertThat(dto.clothes()).hasSize(1);

        ClothesDto clothesDto = dto.clothes().get(0);
        assertThat(clothesDto.id()).isEqualTo(testClothes.getId());
        assertThat(clothesDto.ownerId()).isEqualTo(testUser.getId());
        assertThat(clothesDto.name()).isEqualTo(testClothes.getName());
        assertThat(clothesDto.imageUrl()).isEqualTo(testClothes.getImageUrl());
        assertThat(clothesDto.type()).isEqualTo(testClothes.getType());
        assertThat(clothesDto.attributes()).isEmpty();
    }
}