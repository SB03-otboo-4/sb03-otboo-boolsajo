package com.sprint.otboo.clothing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.mapper.ClothesAttributeMapper;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeRepository;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.clothing.service.ClothesServiceImpl;
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
}