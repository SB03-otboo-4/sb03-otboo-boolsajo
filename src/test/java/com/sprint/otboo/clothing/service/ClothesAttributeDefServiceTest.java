package com.sprint.otboo.clothing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefUpdateRequest;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import java.time.Instant;
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

    @Test
    void 모든의상속성정의를_이름기준_오름차순으로_조회() {
        // given: 의상 속성 정의 두 개 생성 및 저장
        ClothesAttributeDef def1 = ClothesAttributeDef.builder()
            .name("색상")
            .selectValues("빨강,파랑,노랑")
            .build();

        ClothesAttributeDef def2 = ClothesAttributeDef.builder()
            .name("사이즈")
            .selectValues("S,M,L")
            .build();

        repository.saveAll(List.of(def1, def2));

        // when: 이름 기준 오름차순으로 조회
        List<ClothesAttributeDefDto> result =
            clothesAttributeDefService.listAttributeDefs("name", "ASCENDING", null);

        // then: 조회 결과 검증
        assertThat(result).extracting(ClothesAttributeDefDto::name)
            .containsExactly("사이즈", "색상");
    }

    @Test
    void 의상속성정의_조회_키워드필터적용() {
        // given: 의상 속성 정의 두 개 생성 및 저장
        ClothesAttributeDef def1 = ClothesAttributeDef.builder()
            .name("색상")
            .selectValues("빨강,파랑")
            .build();

        ClothesAttributeDef def2 = ClothesAttributeDef.builder()
            .name("사이즈")
            .selectValues("S,M,L")
            .build();

        repository.saveAll(List.of(def1, def2));

        // when: 이름 기준 오름차순으로 조회하며 키워드 "색" 적용
        List<ClothesAttributeDefDto> result =
            clothesAttributeDefService.listAttributeDefs("name", "ASCENDING", "색");

        // then: 필터 결과 검증
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("색상");
    }

    @Test
    void 의상속성정의_조회_생성일기준_내림차순정렬() {
        // given: 의상 속성 정의 두 개 생성 및 저장
        Instant now = Instant.now();

        ClothesAttributeDef def1 = ClothesAttributeDef.builder()
            .name("색상")
            .selectValues("빨강,파랑")
            .createdAt(now.minusSeconds(10))
            .build();

        ClothesAttributeDef def2 = ClothesAttributeDef.builder()
            .name("사이즈")
            .selectValues("S,M,L")
            .createdAt(now)
            .build();

        repository.saveAll(List.of(def1, def2));

        // when: 생성일 기준 내림차순으로 조회
        List<ClothesAttributeDefDto> result =
            clothesAttributeDefService.listAttributeDefs("createdAt", "DESCENDING", null);

        // then: 나중에 저장된 def2가 먼저 반환되는지 검증
        assertThat(result).extracting(ClothesAttributeDefDto::name)
            .containsExactly("사이즈", "색상");
    }

    @Test
    void 의상속성정의_조회_허용되지않는_sortBy_예외() {
        // when & then: 허용되지 않는 sortBy 입력 시 예외 발생 검증
        CustomException thrown = assertThrows(
            CustomException.class,
            () -> clothesAttributeDefService.listAttributeDefs("invalidColumn", "ASCENDING", null)
        );
        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.INVALID_SORT_BY);
    }

    @Test
    void 의상속성정의_조회_허용되지않는_sortDirection_처리() {
        // given: 의상 속성 정의 생성 및 저장
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .name("색상")
            .selectValues("빨강,파랑")
            .build();
        repository.save(def);

        // when: 잘못된 sortDirection 입력
        List<ClothesAttributeDefDto> result =
            clothesAttributeDefService.listAttributeDefs("name", "INVALID", null);

        // then: 기본 ASC 정렬 적용 확인
        assertThat(result).extracting(ClothesAttributeDefDto::name)
            .containsExactly("색상");
    }

    @Test
    void 의상속성정의_조회_null_sortBy_sortDirection_기본정렬() {
        // given: 의상 속성 정의 두 개 생성 및 저장
        ClothesAttributeDef def1 = ClothesAttributeDef.builder()
            .name("색상")
            .selectValues("빨강,파랑")
            .build();
        ClothesAttributeDef def2 = ClothesAttributeDef.builder()
            .name("사이즈")
            .selectValues("S,M,L")
            .build();
        repository.saveAll(List.of(def1, def2));

        // when: sortBy, sortDirection 모두 null 입력
        List<ClothesAttributeDefDto> result =
            clothesAttributeDefService.listAttributeDefs(null, null, null);

        // then: 기본 이름 기준 오름차순 정렬 확인
        assertThat(result).extracting(ClothesAttributeDefDto::name)
            .containsExactly("사이즈", "색상");
    }
}