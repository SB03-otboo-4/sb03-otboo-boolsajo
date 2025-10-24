package com.sprint.otboo.clothing.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.mapper.scraper.ClothesTypeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClothesTypeMapper 단위 테스트")
public class ClothesTypeMapperTest {

    @Test
    void 한글_카테고리_매핑() {
        // given: 한글 카테고리 문자열
        // when: ClothesTypeMapper로 변환
        ClothesType outer = ClothesTypeMapper.mapToClothesType("자켓");
        ClothesType top = ClothesTypeMapper.mapToClothesType("셔츠");
        ClothesType bottom = ClothesTypeMapper.mapToClothesType("청바지");
        ClothesType shoes = ClothesTypeMapper.mapToClothesType("신발");
        ClothesType hat = ClothesTypeMapper.mapToClothesType("모자");
        ClothesType bag = ClothesTypeMapper.mapToClothesType("가방");
        ClothesType accessory = ClothesTypeMapper.mapToClothesType("악세서리");
        ClothesType scarf = ClothesTypeMapper.mapToClothesType("스카프");
        ClothesType underwear = ClothesTypeMapper.mapToClothesType("속옷");
        ClothesType socks = ClothesTypeMapper.mapToClothesType("양말");
        ClothesType dress = ClothesTypeMapper.mapToClothesType("원피스");

        // then: 올바른 ClothesType 반환 확인
        assertThat(outer).isEqualTo(ClothesType.OUTER);
        assertThat(top).isEqualTo(ClothesType.TOP);
        assertThat(bottom).isEqualTo(ClothesType.BOTTOM);
        assertThat(shoes).isEqualTo(ClothesType.SHOES);
        assertThat(hat).isEqualTo(ClothesType.HAT);
        assertThat(bag).isEqualTo(ClothesType.BAG);
        assertThat(accessory).isEqualTo(ClothesType.ACCESSORY);
        assertThat(scarf).isEqualTo(ClothesType.SCARF);
        assertThat(underwear).isEqualTo(ClothesType.UNDERWEAR);
        assertThat(socks).isEqualTo(ClothesType.SOCKS);
        assertThat(dress).isEqualTo(ClothesType.DRESS);
    }

    @Test
    void 영문_카테고리_매핑() {
        // given: 영어 카테고리 문자열
        // when: ClothesTypeMapper로 변환
        ClothesType outer = ClothesTypeMapper.mapToClothesType("jacket");
        ClothesType top = ClothesTypeMapper.mapToClothesType("hoodie");
        ClothesType bottom = ClothesTypeMapper.mapToClothesType("jeans");
        ClothesType shoes = ClothesTypeMapper.mapToClothesType("sneakers");
        ClothesType hat = ClothesTypeMapper.mapToClothesType("hat");
        ClothesType bag = ClothesTypeMapper.mapToClothesType("bag");
        ClothesType accessory = ClothesTypeMapper.mapToClothesType("accessory");
        ClothesType scarf = ClothesTypeMapper.mapToClothesType("scarf");
        ClothesType underwear = ClothesTypeMapper.mapToClothesType("underwear");
        ClothesType socks = ClothesTypeMapper.mapToClothesType("socks");
        ClothesType dress = ClothesTypeMapper.mapToClothesType("dress");

        // then: 올바른 ClothesType 반환 확인
        assertThat(outer).isEqualTo(ClothesType.OUTER);
        assertThat(top).isEqualTo(ClothesType.TOP);
        assertThat(bottom).isEqualTo(ClothesType.BOTTOM);
        assertThat(shoes).isEqualTo(ClothesType.SHOES);
        assertThat(hat).isEqualTo(ClothesType.HAT);
        assertThat(bag).isEqualTo(ClothesType.BAG);
        assertThat(accessory).isEqualTo(ClothesType.ACCESSORY);
        assertThat(scarf).isEqualTo(ClothesType.SCARF);
        assertThat(underwear).isEqualTo(ClothesType.UNDERWEAR);
        assertThat(socks).isEqualTo(ClothesType.SOCKS);
        assertThat(dress).isEqualTo(ClothesType.DRESS);
    }

    @Test
    @DisplayName("혼합 문자열 매핑")
    void 혼합_문자열_매핑() {
        // given: 혼합 카테고리 문자열
        // when: ClothesTypeMapper로 변환
        ClothesType outer = ClothesTypeMapper.mapToClothesType("후드집업");
        ClothesType top = ClothesTypeMapper.mapToClothesType("tanktop");
        ClothesType bottom = ClothesTypeMapper.mapToClothesType("shorts");
        ClothesType shoes = ClothesTypeMapper.mapToClothesType("high-top");

        // then: 올바른 ClothesType 반환 확인
        assertThat(outer).isEqualTo(ClothesType.OUTER);
        assertThat(top).isEqualTo(ClothesType.TOP);
        assertThat(bottom).isEqualTo(ClothesType.BOTTOM);
        assertThat(shoes).isEqualTo(ClothesType.SHOES);
    }

    @Test
    @DisplayName("알 수 없는 문자열 및 null 처리")
    void 알수없는_문자열_및_null_처리() {
        // given: 알 수 없거나 처리할 수 없는 문자열
        // when: ClothesTypeMapper로 변환
        ClothesType empty = ClothesTypeMapper.mapToClothesType("");
        ClothesType unknown = ClothesTypeMapper.mapToClothesType("unknown category");
        ClothesType nullValue = ClothesTypeMapper.mapToClothesType(null);

        // then: 올바른 ClothesType 반환 확인
        assertThat(empty).isEqualTo(ClothesType.ETC);
        assertThat(unknown).isEqualTo(ClothesType.ETC);
        assertThat(nullValue).isEqualTo(ClothesType.ETC);
    }

    @Test
    @DisplayName("대소문자 무시 매핑")
    void 대소문자_무시_매핑() {
        // given: 대소문자가 제각각인 문자열
        // when: ClothesTypeMapper로 변환
        ClothesType outer = ClothesTypeMapper.mapToClothesType("JACKET");
        ClothesType bottom = ClothesTypeMapper.mapToClothesType("JeAnS");
        ClothesType shoes = ClothesTypeMapper.mapToClothesType("Sneakers");

        // then: 올바른 ClothesType 반환 확인
        assertThat(outer).isEqualTo(ClothesType.OUTER);
        assertThat(bottom).isEqualTo(ClothesType.BOTTOM);
        assertThat(shoes).isEqualTo(ClothesType.SHOES);
    }

    @Test
    @DisplayName("단어 경계 및 특수 문자 처리")
    void 단어경계_및_특수문자_처리() {
        // given: 단어 경계 및 특수 문자열
        // when & then: ClothesTypeMapper로 변환 후 올바른지 검증
        assertThat(ClothesTypeMapper.mapToClothesType("laptop")).isEqualTo(ClothesType.ETC);
        assertThat(ClothesTypeMapper.mapToClothesType("crop top")).isEqualTo(ClothesType.TOP);
        assertThat(ClothesTypeMapper.mapToClothesType("t shirt")).isEqualTo(ClothesType.TOP);
        assertThat(ClothesTypeMapper.mapToClothesType("t-shirt")).isEqualTo(ClothesType.TOP);

        assertThat(ClothesTypeMapper.mapToClothesType("one-piece")).isEqualTo(ClothesType.DRESS);
        assertThat(ClothesTypeMapper.mapToClothesType("one piece")).isEqualTo(ClothesType.DRESS);

        assertThat(ClothesTypeMapper.mapToClothesType("high-top")).isEqualTo(ClothesType.SHOES);
        assertThat(ClothesTypeMapper.mapToClothesType("high top")).isEqualTo(ClothesType.SHOES);

        assertThat(ClothesTypeMapper.mapToClothesType("bottoms")).isEqualTo(ClothesType.BOTTOM);
        assertThat(ClothesTypeMapper.mapToClothesType("somebottom")).isEqualTo(ClothesType.ETC);
    }
}