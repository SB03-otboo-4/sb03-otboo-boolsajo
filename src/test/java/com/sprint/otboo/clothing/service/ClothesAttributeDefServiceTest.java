package com.sprint.otboo.clothing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefUpdateRequest;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.event.ClothesAttributeDefCreatedEvent;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.mapper.ClothesAttributeDefMapper;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.service.impl.ClothesAttributeDefServiceImpl;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("의상 속성 서비스 테스트( ADMIN )")
public class ClothesAttributeDefServiceTest {

    @Mock
    private ClothesAttributeDefRepository repository;

    @Mock
    private ClothesAttributeDefMapper clothesAttributeDefMapper;

    @Mock
    private ClothesMapper clothesMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ClothesAttributeDefServiceImpl clothesAttributeDefService;

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
        // given: 유효한 생성 요청 및 repository, mapper, eventPublisher mocking
        ClothesAttributeDefCreateRequest request =
            new ClothesAttributeDefCreateRequest("색상", List.of("빨강", "파랑"));

        // repository.save() stub
        when(repository.save(any())).thenAnswer(invocation -> {
            ClothesAttributeDef arg = invocation.getArgument(0);
            return ClothesAttributeDef.builder()
                .id(UUID.randomUUID())
                .name(arg.getName())
                .selectValues(arg.getSelectValues())
                .build();
        });

        // Mapper stub
        doAnswer(invocation -> {
            ClothesAttributeDef arg = invocation.getArgument(0);
            return new ClothesAttributeDefDto(
                arg.getId(),
                arg.getName(),
                Arrays.asList(arg.getSelectValues().split(",")),
                Instant.now()
            );
        }).when(clothesMapper).toClothesAttributeDefDto(any());

        // eventPublisher stub
        doNothing().when(eventPublisher).publishEvent(any());

        // when: 서비스 호출
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
            .id(UUID.randomUUID())
            .name("색상")
            .selectValues("빨강,파랑")
            .build();

        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(
            "사이즈",
            List.of("S", "M", "L")
        );

        // repository.findById() stub
        when(repository.findById(existingDef.getId())).thenReturn(Optional.of(existingDef));

        // repository.save() stub (수정 후 반환)
        when(repository.save(any(ClothesAttributeDef.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // clothesMapper.toClothesAttributeDefDto() stub
        when(clothesMapper.toClothesAttributeDefDto(any(ClothesAttributeDef.class)))
            .thenAnswer(invocation -> {
                ClothesAttributeDef arg = invocation.getArgument(0);
                return new ClothesAttributeDefDto(
                    arg.getId(),
                    arg.getName(),
                    Arrays.asList(arg.getSelectValues().split(",")),
                    Instant.now()
                );
            });

        // when: 수정 요청 실행
        var result = clothesAttributeDefService.updateAttributeDef(
            existingDef.getId(),
            request
        );

        // then: 수정 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("사이즈");
        assertThat(result.selectableValues()).containsExactly("S", "M", "L");
    }

    @Test
    void 속성_이름이_없으면_수정_실패() {
        // given: 기존 엔티티와 이름이 없는 수정 요청
        ClothesAttributeDef existingDef = ClothesAttributeDef.builder()
            .name("색상")
            .selectValues("빨강,파랑")
            .build();
        repository.save(existingDef);

        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(
            null,
            List.of("S", "M")
        );

        // when: 수정 시도
        ClothesValidationException thrown = assertThrows(
            ClothesValidationException.class,
            () -> clothesAttributeDefService.updateAttributeDef(existingDef.getId(), request)
        );
        // then: 예외 발생 검증
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
    void 모든의상_속성정의를_이름기준_오름차순으로_조회() {
        // given: 저장된 의상 속성 정의 리스트 및 repository, mapper mocking
        ClothesAttributeDef def1 = ClothesAttributeDef.builder()
            .id(UUID.randomUUID())
            .name("색상")
            .selectValues("빨강,파랑,노랑")
            .build();

        ClothesAttributeDef def2 = ClothesAttributeDef.builder()
            .id(UUID.randomUUID())
            .name("사이즈")
            .selectValues("S,M,L")
            .build();

        List<ClothesAttributeDef> savedDefs = List.of(def1, def2);

        // repository.findAll(Sort) stub
        when(repository.findAll(any(Sort.class)))
            .thenAnswer(invocation -> {
                List<ClothesAttributeDef> list = new ArrayList<>(savedDefs);
                // 이름 기준 오름차순 정렬
                list.sort(Comparator.comparing(ClothesAttributeDef::getName));
                return list;
            });

        // mapper.toDto stub
        when(clothesAttributeDefMapper.toDto(any(ClothesAttributeDef.class)))
            .thenAnswer(invocation -> {
                ClothesAttributeDef arg = invocation.getArgument(0);
                return new ClothesAttributeDefDto(
                    arg.getId(),
                    arg.getName(),
                    Arrays.asList(arg.getSelectValues().split(",")),
                    Instant.now()
                );
            });

        // when: 서비스에서 모든 의상 속성 정의 조회
        List<ClothesAttributeDefDto> result =
            clothesAttributeDefService.listAttributeDefs("name", "ASCENDING", null);

        // then: 결과가 이름 기준 오름차순으로 정렬되었는지 검증
        assertThat(result).extracting(ClothesAttributeDefDto::name)
            .containsExactly("사이즈", "색상");
    }

    @Test
    void 의상속성정의_조회_키워드필터적용() {
        // given: 의상 속성 정의 리스트와 키워드 "색", repository, mapper mocking
        ClothesAttributeDef def1 = ClothesAttributeDef.builder()
            .id(UUID.randomUUID())
            .name("색상")
            .selectValues("빨강,파랑")
            .build();
        ClothesAttributeDef def2 = ClothesAttributeDef.builder()
            .id(UUID.randomUUID())
            .name("사이즈")
            .selectValues("S,M,L")
            .build();
        List<ClothesAttributeDef> allDefs = List.of(def1, def2);

        // repository.findByNameOrSelectValuesContainingIgnoreCase(keyword, sort) mocking
        when(repository.findByNameOrSelectValuesContainingIgnoreCase(eq("색"), any(Sort.class)))
            .thenAnswer(invocation -> List.of(def1));

        // mapper.toDto mocking
        when(clothesAttributeDefMapper.toDto(any(ClothesAttributeDef.class)))
            .thenAnswer(invocation -> {
                ClothesAttributeDef arg = invocation.getArgument(0);
                return new ClothesAttributeDefDto(
                    arg.getId(),
                    arg.getName(),
                    Arrays.asList(arg.getSelectValues().split(",")),
                    Instant.now()
                );
            });

        // when: 키워드 "색"으로 조회
        List<ClothesAttributeDefDto> result =
            clothesAttributeDefService.listAttributeDefs("name", "ASCENDING", "색");

        // then: 필터 결과 검증
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("색상");
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
        // given: 의상 속성 정의 리스트 및 잘못된 sortDirection, repository, mapper mocking
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(UUID.randomUUID())
            .name("색상")
            .selectValues("빨강,파랑")
            .build();

        List<ClothesAttributeDef> savedDefs = List.of(def);

        // repository.findAll 호출 시 저장된 리스트 반환
        when(repository.findAll(any(Sort.class)))
            .thenAnswer(invocation -> {
                // sortDirection이 INVALID면 기본 ASC로 정렬
                List<ClothesAttributeDef> list = new ArrayList<>(savedDefs);
                list.sort(Comparator.comparing(ClothesAttributeDef::getName));
                return list;
            });

        // mapper.toDto mocking
        when(clothesAttributeDefMapper.toDto(any(ClothesAttributeDef.class)))
            .thenAnswer(invocation -> {
                ClothesAttributeDef arg = invocation.getArgument(0);
                return new ClothesAttributeDefDto(
                    arg.getId(),
                    arg.getName(),
                    Arrays.asList(arg.getSelectValues().split(",")),
                    Instant.now()
                );
            });

        // when: 잘못된 sortDirection 입력
        List<ClothesAttributeDefDto> result =
            clothesAttributeDefService.listAttributeDefs("name", "INVALID", null);

        // then: 기본 ASC 정렬 적용 확인
        assertThat(result).extracting(ClothesAttributeDefDto::name)
            .containsExactly("색상");
    }

    @Test
    void 의상속성정의_조회_null_sortBy_sortDirection_기본정렬() {
        // given: 저장된 의상 속성 정의 리스트 및 repository, mapper mocking
        List<ClothesAttributeDef> savedDefs = List.of(
            ClothesAttributeDef.builder().id(UUID.randomUUID()).name("색상").selectValues("빨강,파랑").build(),
            ClothesAttributeDef.builder().id(UUID.randomUUID()).name("사이즈").selectValues("S,M,L").build()
        );

        when(repository.findAll(any(Sort.class)))
            .thenAnswer(invocation -> {
                Sort sort = invocation.getArgument(0);
                List<ClothesAttributeDef> list = new ArrayList<>(savedDefs);
                // name 기준 오름차순 정렬
                list.sort(Comparator.comparing(ClothesAttributeDef::getName));
                return list;
            });

        // mapper mocking
        when(clothesAttributeDefMapper.toDto(any(ClothesAttributeDef.class)))
            .thenAnswer(invocation -> {
                ClothesAttributeDef arg = invocation.getArgument(0);
                return new ClothesAttributeDefDto(
                    arg.getId(),
                    arg.getName(),
                    Arrays.asList(arg.getSelectValues().split(",")),
                    Instant.now()
                );
            });

        // when: null sortBy, null sortDirection
        List<ClothesAttributeDefDto> result = clothesAttributeDefService.listAttributeDefs(null, null, null);

        // then: 기본 ASC 정렬 적용 확인
        assertThat(result).extracting(ClothesAttributeDefDto::name)
            .containsExactly("사이즈", "색상");
    }

    @Test
    void 의상속성정의_삭제_성공() {
        // given: 삭제 대상 의상 속성 정의 생성 및 repository mocking
        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name("색상")
            .selectValues("빨강,파랑")
            .build();

        // Mock 동작 정의
        when(repository.findById(defId)).thenReturn(Optional.of(def));
        doNothing().when(repository).delete(def);

        // when: 삭제 메서드 실행
        clothesAttributeDefService.deleteAttributeDef(defId);

        // then: delete 호출 검증
        verify(repository).delete(def);
    }

    @Test
    void 의상속성정의_삭제_실패_존재하지않는ID() {
        // given: 존재하지 않는 UUID
        UUID randomId = UUID.randomUUID();

        // when & then: 삭제 시 CustomException 발생 검증
        CustomException thrown = assertThrows(
            CustomException.class,
            () -> clothesAttributeDefService.deleteAttributeDef(randomId)
        );
        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}