package com.sprint.otboo.clothing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefUpdateRequest;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("의상 속성 서비스 테스트( ADMIN )")
public class ClothesAttributeDefServiceTest {

    @Autowired
    private ClothesAttributeDefService clothesAttributeDefService;

    @Autowired
    private ClothesAttributeDefRepository repository;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void 속성_이름이_없으면_등록_실패() {
        // given: 이름이 null인 요청
        ClothesAttributeDefCreateRequest request =
            new ClothesAttributeDefCreateRequest(null, List.of("빨강", "파랑"));

        // when: 요청 실행
        ClothesValidationException thrown = assertThrows(
            ClothesValidationException.class,
            () -> clothesAttributeDefService.createAttributeDef(request)
        );

        // then: 예외 메시지 검증
        assertThat(thrown.getMessage()).isEqualTo("속성 이름은 필수입니다");
    }

    @Test
    void 유효한_요청이면_의상_속성_정의_등록_성공() {
        // given: 유효한 요청
        ClothesAttributeDefCreateRequest request =
            new ClothesAttributeDefCreateRequest("색상", List.of("빨강", "파랑"));

        // when: 요청 실행
        var result = clothesAttributeDefService.createAttributeDef(request);

        // then: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("색상");
        assertThat(result.selectableValues()).containsExactly("빨강", "파랑");
    }

    @Test
    void 의상_속성_정의_수정_성공() {
        // given: 기존 엔티티와 업데이트 요청
        ClothesAttributeDef existingDef = ClothesAttributeDef.builder()
            .name("색상")
            .selectValues("빨강,파랑")
            .build();
        repository.save(existingDef);

        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(
            "사이즈",
            List.of("S", "M", "L")
        );

        // when: 수정 메서드 실행
        var result = clothesAttributeDefService.updateAttributeDef(
            existingDef.getId(),
            request
        );

        // then: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("사이즈");
        assertThat(result.selectableValues()).containsExactly("S", "M", "L");
    }

    @Test
    void 속성_이름이_없으면_수정_실패() {
        // given
        ClothesAttributeDef existingDef = ClothesAttributeDef.builder()
            .name("색상")
            .selectValues("빨강,파랑")
            .build();
        repository.save(existingDef);

        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(
            null,
            List.of("S", "M")
        );

        // when & then: 예외 발생 검증
        ClothesValidationException thrown = assertThrows(
            ClothesValidationException.class,
            () -> clothesAttributeDefService.updateAttributeDef(existingDef.getId(), request)
        );

        assertThat(thrown.getMessage()).isEqualTo("속성 이름은 필수입니다");
    }

    @Test
    void 존재하지_않는_의상_속성_정의_수정_시_실패() {
        // given: 랜덤 UUID와 요청
        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(
            "사이즈",
            List.of("S", "M", "L")
        );

        // when & then: 예외 발생 검증
        ClothesValidationException thrown = assertThrows(
            ClothesValidationException.class,
            () -> clothesAttributeDefService.updateAttributeDef(UUID.randomUUID(), request)
        );

        assertThat(thrown.getMessage()).isEqualTo("존재하지 않는 의상 속성 정의");
    }

    @Test
    void 속성값이_없으면_수정_실패() {
        // given: 기존 엔티티
        ClothesAttributeDef existingDef = ClothesAttributeDef.builder()
            .name("색상")
            .selectValues("빨강,파랑")
            .build();
        repository.save(existingDef);

        // selectableValues가 빈 리스트
        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(
            "사이즈",
            Collections.emptyList()
        );

        // when & then: 예외 발생 검증
        ClothesValidationException thrown = assertThrows(
            ClothesValidationException.class,
            () -> clothesAttributeDefService.updateAttributeDef(existingDef.getId(), request)
        );

        assertThat(thrown.getMessage()).isEqualTo("속성 값은 최소 1개 이상 필요합니다");
    }

    @Test
    void 속성값이_null이면_수정_실패() {
        // given: 기존 엔티티
        ClothesAttributeDef existingDef = ClothesAttributeDef.builder()
            .name("색상")
            .selectValues("빨강,파랑")
            .build();
        repository.save(existingDef);

        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(
            "사이즈",
            null
        );

        // when & then: 예외 발생 검증
        ClothesValidationException thrown = assertThrows(
            ClothesValidationException.class,
            () -> clothesAttributeDefService.updateAttributeDef(existingDef.getId(), request)
        );

        assertThat(thrown.getMessage()).isEqualTo("속성 값은 최소 1개 이상 필요합니다");
    }
}