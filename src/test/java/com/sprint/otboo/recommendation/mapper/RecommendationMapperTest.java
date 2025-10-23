package com.sprint.otboo.recommendation.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeWithDefDto;
import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
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
import org.mapstruct.factory.Mappers;

@DisplayName("RecommendationMapper 단위 테스트")
public class RecommendationMapperTest {

    private RecommendationMapper recommendationMapper;

    private User testUser;
    private Clothes testClothes;
    private RecommendationClothes testRecommendationClothes;
    private Recommendation testRecommendation;

    @BeforeEach
    void setUp() {
        // given: MapStruct 인터페이스, 테스트용 사용자, 의상, 추천 관계, Recommendation 엔티티 생성
        recommendationMapper = Mappers.getMapper(RecommendationMapper.class);

        testUser = User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .build();

        ClothesAttribute attribute = ClothesAttribute.builder()
            .id(UUID.randomUUID())
            .value("LIGHT")
            .definition(ClothesAttributeDef.builder()
                .id(UUID.randomUUID())
                .name("두께")
                .selectValues("LIGHT,MEDIUM,HEAVY")
                .build())
            .build();

        testClothes = Clothes.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .name("Top 1")
            .imageUrl("top1.jpg")
            .type(ClothesType.TOP)
            .attributes(List.of(attribute))
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

        OotdDto ootdDto = dto.clothes().get(0);
        assertThat(ootdDto.clothesId()).isEqualTo(testClothes.getId());
        assertThat(ootdDto.name()).isEqualTo(testClothes.getName());
        assertThat(ootdDto.imageUrl()).isEqualTo(testClothes.getImageUrl());
        assertThat(ootdDto.type()).isEqualTo(testClothes.getType());

        // attribute 변환 확인
        assertThat(ootdDto.attributes()).hasSize(1);
        ClothesAttributeWithDefDto attrDto = ootdDto.attributes().get(0);
        ClothesAttribute attr = testClothes.getAttributes().get(0);

        assertThat(attrDto.value()).isEqualTo(attr.getValue());
        assertThat(attrDto.definitionId()).isEqualTo(attr.getDefinition().getId());
        assertThat(attrDto.definitionName()).isEqualTo(attr.getDefinition().getName());
        assertThat(attrDto.selectableValues()).containsExactly("LIGHT", "MEDIUM", "HEAVY");
    }
}