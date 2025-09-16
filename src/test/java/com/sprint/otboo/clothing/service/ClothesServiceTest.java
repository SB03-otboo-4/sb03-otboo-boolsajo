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
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.mapper.ClothesAttributeMapper;
import com.sprint.otboo.clothing.mapper.ClothesMapperImpl;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.repository.ClothesAttributeRepository;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.clothing.storage.FileStorageService;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("의상 등록 서비스")
public class ClothesServiceTest {

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private ClothesAttributeRepository clothesAttributeRepository;

    @Mock
    private ClothesAttributeDefRepository defRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ClothesServiceImpl clothesService;

    @BeforeEach
    void setUp() throws Exception {
        ClothesAttributeMapper clothesAttributeMapper = Mappers.getMapper(ClothesAttributeMapper.class);

        ClothesMapperImpl clothesMapper;
        try {
            Constructor<ClothesMapperImpl> constructor = ClothesMapperImpl.class.getDeclaredConstructor(ClothesAttributeMapper.class);
            clothesMapper = constructor.newInstance(clothesAttributeMapper);
        } catch (NoSuchMethodException e) {
            clothesMapper = new ClothesMapperImpl();
            Field field = ClothesMapperImpl.class.getDeclaredField("clothesAttributeMapper");
            field.setAccessible(true);
            field.set(clothesMapper, clothesAttributeMapper);
        }

        this.clothesService = new ClothesServiceImpl(
            clothesRepository,
            clothesAttributeRepository,
            clothesMapper,
            clothesAttributeMapper,
            fileStorageService,
            userRepository,
            defRepository
        );
    }


    @Test
    void 옷_등록_성공() {
        // given
        UUID ownerId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();
        var attrDto = new ClothesAttributeDto(defId, "Black");
        var request = new ClothesCreateRequest(ownerId, "화이트 티셔츠", ClothesType.TOP, List.of(attrDto));
        MultipartFile image = null;

        var user = User.builder().id(ownerId).build();
        var def = ClothesAttributeDef.builder().id(defId).name("색상").build();

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(defRepository.findById(defId)).thenReturn(Optional.of(def));
        when(clothesRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.upload(image)).thenReturn("/uploads/test.png");

        // when
        ClothesDto result = clothesService.createClothes(request, image);

        // then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("화이트 티셔츠");
        assertThat(result.type()).isEqualTo(ClothesType.TOP);
        assertThat(result.attributes()).hasSize(1);
        assertThat(result.attributes().get(0).definitionId()).isEqualTo(defId);
        assertThat(result.attributes().get(0).value()).isEqualTo("Black");

        verify(clothesRepository, times(1)).save(any());
        verify(fileStorageService, times(1)).upload(image);
    }


    @Test
    void 요청DTO가_null이면_예외발생() {
        // given
        ClothesCreateRequest request = null;

        // when & then
        assertThatThrownBy(() -> clothesService.createClothes(request, null))
            .isInstanceOf(ClothesValidationException.class)
            .hasMessage("요청 데이터가 존재하지 않음");
    }

    @Test
    void 의상_소유주_Id가_null이면_예외발생() {
        // given
        var request = new ClothesCreateRequest(null, "티셔츠", ClothesType.TOP, List.of());

        // when & then
        assertThatThrownBy(() -> clothesService.createClothes(request, null))
            .isInstanceOf(ClothesValidationException.class)
            .hasMessage("의상 소유자의 ID가 필요합니다");
    }

    @Test
    void name이_null또는공백이면_예외발생() {
        // given
        var request1 = new ClothesCreateRequest(UUID.randomUUID(), null, ClothesType.TOP, List.of());
        var request2 = new ClothesCreateRequest(UUID.randomUUID(), "   ", ClothesType.TOP, List.of());

        // when & then
        assertThatThrownBy(() -> clothesService.createClothes(request1, null))
            .isInstanceOf(ClothesValidationException.class)
            .hasMessage("의상 이름은 필수입니다");

        assertThatThrownBy(() -> clothesService.createClothes(request2, null))
            .isInstanceOf(ClothesValidationException.class)
            .hasMessage("의상 이름은 필수입니다");
    }

    @Test
    void type이_null이면_예외발생() {
        // given
        var request = new ClothesCreateRequest(UUID.randomUUID(), "티셔츠", null, List.of());

        // when & then
        assertThatThrownBy(() -> clothesService.createClothes(request, null))
            .isInstanceOf(ClothesValidationException.class)
            .hasMessage("의상 타입은 필수입니다");
    }

    @Test
    void 속성리스트가_null이거나_비어도_정상등록() {
        // given
        UUID ownerId = UUID.randomUUID();
        var request = new ClothesCreateRequest(ownerId, "티셔츠", ClothesType.TOP, null);
        var user = User.builder().id(ownerId).build();

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));
        when(clothesRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        ClothesDto result = clothesService.createClothes(request, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.attributes()).isEmpty();
    }
}