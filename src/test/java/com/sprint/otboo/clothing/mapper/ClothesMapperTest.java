package com.sprint.otboo.clothing.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeWithDefDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.feed.entity.FeedClothes;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("ClothesMapper 매핑 테스트")
public class ClothesMapperTest {

    @Autowired
    private ClothesMapper clothesMapper;

    @Autowired
    private ClothesAttributeMapper mapper;

    @Test
    void 의상_속성_정의_엔티티를_DTO로_변환() {
        // given: ClothesAttributeDef 엔티티
        ClothesAttributeDef entity = ClothesAttributeDef.builder()
            .id(UUID.randomUUID())
            .name("색상")
            .selectValues("빨강,파랑,초록")
            .createdAt(Instant.now())
            .build();

        // when: 매핑 수행
        ClothesAttributeDefDto dto = clothesMapper.toClothesAttributeDefDto(entity);

        // then: 변환 검증
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(entity.getId());
        assertThat(dto.name()).isEqualTo("색상");
        assertThat(dto.selectableValues()).containsExactly("빨강", "파랑", "초록");
        assertThat(dto.createdAt()).isEqualTo(entity.getCreatedAt());
    }

    @Test
    void 의상_속성_엔티티를_DTO로_변환() {
        // given: ClothesAttribute 엔티티
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(UUID.randomUUID())
            .name("사이즈")
            .selectValues("S,M,L")
            .build();

        ClothesAttribute attribute = ClothesAttribute.builder()
            .definition(def)
            .value("M")
            .build();

        // when: 매핑 수행
        ClothesAttributeWithDefDto dto = clothesMapper.toClothesAttributeWithDefDto(attribute);

        // then: 변환 검증
        assertThat(dto).isNotNull();
        assertThat(dto.definitionId()).isEqualTo(def.getId());
        assertThat(dto.definitionName()).isEqualTo("사이즈");
        assertThat(dto.selectableValues()).containsExactly("S", "M", "L");
        assertThat(dto.value()).isEqualTo("M");
    }

    @Test
    void 의상_엔티티를_DTO로_변환() {
        // given: Clothes 엔티티
        Clothes clothes = Clothes.builder()
            .id(UUID.randomUUID())
            .name("티셔츠")
            .imageUrl("http://image.url")
            .type(com.sprint.otboo.clothing.entity.ClothesType.TOP)
            .user(null)
            .build();

        // when: 매핑 수행
        ClothesDto dto = clothesMapper.toDto(clothes);

        // then: 변환 검증
        assertThat(dto).isNotNull();
        assertThat(dto.name()).isEqualTo("티셔츠");
        assertThat(dto.imageUrl()).isEqualTo("http://image.url");
        assertThat(dto.type()).isEqualTo(clothes.getType());
    }

    @Test
    void 피드_의상_엔티티를_OotdDto로_변환() {
        // given: FeedClothes 엔티티
        Clothes clothes = Clothes.builder()
            .id(UUID.randomUUID())
            .name("재킷")
            .imageUrl("http://image2.url")
            .type(com.sprint.otboo.clothing.entity.ClothesType.OUTER)
            .build();

        FeedClothes feedClothes = FeedClothes.builder()
            .clothes(clothes)
            .build();

        // when: 매핑 수행
        OotdDto dto = clothesMapper.toOotdDto(feedClothes);

        // then: 변환 검증
        assertThat(dto).isNotNull();
        assertThat(dto.clothesId()).isEqualTo(clothes.getId());
        assertThat(dto.name()).isEqualTo("재킷");
        assertThat(dto.imageUrl()).isEqualTo("http://image2.url");
        assertThat(dto.type()).isEqualTo(clothes.getType());
    }

    @Test
    void 의상_속성_엔티티를_ClothesAttributeDto로_변환() {
        // given: 의상 속성 엔티티
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(UUID.randomUUID())
            .name("색상")
            .selectValues("빨강,파랑,초록")
            .createdAt(Instant.now())
            .build();

        ClothesAttribute entity = ClothesAttribute.builder()
            .definition(def)
            .value("빨강")
            .createdAt(Instant.now())
            .build();

        // when: 매핑 수행
        ClothesAttributeDto dto = mapper.toDto(entity);

        // then: 변환 검증
        assertThat(dto).isNotNull();
        assertThat(dto.definitionId()).isEqualTo(def.getId());
        assertThat(dto.value()).isEqualTo("빨강");
    }
}