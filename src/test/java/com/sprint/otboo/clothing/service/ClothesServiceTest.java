package com.sprint.otboo.clothing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.mapper.ClothesAttributeMapper;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeRepository;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("의상 등록 서비스")
public class ClothesServiceTest {

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private ClothesAttributeRepository clothesAttributeRepository;

    @InjectMocks
    private ClothesServiceImpl clothesService;

    @BeforeEach
    void setUp() {
        ClothesMapper clothesMapper = Mappers.getMapper(ClothesMapper.class);
        ClothesAttributeMapper clothesAttributeMapper = Mappers.getMapper(ClothesAttributeMapper.class);

        clothesService = new ClothesServiceImpl(
            clothesRepository,
            clothesAttributeRepository,
            clothesMapper,
            clothesAttributeMapper
        );
    }

    @Test
    void 옷_등록_성공() {
        // given
        UUID ownerId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();
        var attrDto = new ClothesAttributeDto(defId, "Black");
        var request = new ClothesCreateRequest(ownerId, "화이트 티셔츠", ClothesType.TOP, List.of(attrDto));

        when(clothesRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clothesAttributeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        ClothesDto result = clothesService.createClothes(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("화이트 티셔츠");
        assertThat(result.type()).isEqualTo(ClothesType.TOP);
        assertThat(result.attributes()).hasSize(1);
        assertThat(result.attributes().get(0).definitionId()).isEqualTo(defId);
        assertThat(result.attributes().get(0).value()).isEqualTo("Black");

        verify(clothesRepository, times(1)).save(any());
        verify(clothesAttributeRepository, times(1)).saveAll(any());
    }

    @Test
    void 요청DTO가_null이면_예외발생() {
        // given
        ClothesCreateRequest request = null;

        // when & then
        assertThatThrownBy(() -> clothesService.createClothes(request))
            .isInstanceOf(ClothesValidationException.class)
            .hasMessage("요청 데이터가 존재하지 않음");
    }

    @Test
    void 의상_소유주_Id가_null이면_예외발생() {
        // given
        var request = new ClothesCreateRequest(null, "티셔츠", ClothesType.TOP, List.of());

        // when & then
        assertThatThrownBy(() -> clothesService.createClothes(request))
            .isInstanceOf(ClothesValidationException.class)
            .hasMessage("의상 소유자의 ID가 필요합니다");
    }

    @Test
    void name이_null또는공백이면_예외발생() {
        // given
        var request1 = new ClothesCreateRequest(UUID.randomUUID(), null, ClothesType.TOP, List.of());
        var request2 = new ClothesCreateRequest(UUID.randomUUID(), "   ", ClothesType.TOP, List.of());

        // when & then
        assertThatThrownBy(() -> clothesService.createClothes(request1))
            .isInstanceOf(ClothesValidationException.class)
            .hasMessage("의상 이름은 필수입니다");

        assertThatThrownBy(() -> clothesService.createClothes(request2))
            .isInstanceOf(ClothesValidationException.class)
            .hasMessage("의상 이름은 필수입니다");
    }

    @Test
    void type이_null이면_예외발생() {
        // given
        var request = new ClothesCreateRequest(UUID.randomUUID(), "티셔츠", null, List.of());

        // when & then
        assertThatThrownBy(() -> clothesService.createClothes(request))
            .isInstanceOf(ClothesValidationException.class)
            .hasMessage("의상 타입은 필수입니다");
    }

    @Test
    void 속성리스트가_null이거나_비어도_정상등록() {
        // given
        UUID ownerId = UUID.randomUUID();
        var request = new ClothesCreateRequest(ownerId, "티셔츠", ClothesType.TOP, null);

        when(clothesRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        ClothesDto result = clothesService.createClothes(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.attributes()).isEmpty();
    }

}