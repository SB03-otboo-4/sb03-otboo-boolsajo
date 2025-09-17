package com.sprint.otboo.clothing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeWithDefDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.mapper.ClothesAttributeMapper;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.repository.ClothesAttributeRepository;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.clothing.service.impl.ClothesServiceImpl;
import com.sprint.otboo.clothing.storage.FileStorageService;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feed.entity.FeedClothes;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    // 테스트용 ClothesMapper 구현
    static class TestClothesMapper implements ClothesMapper {
        @Override
        public ClothesDto toDto(Clothes clothes) {
            List<ClothesAttributeDto> attrs = clothes.getAttributes().stream()
                .map(attr -> new ClothesAttributeDto(attr.getDefinition().getId(), attr.getValue()))
                .collect(Collectors.toList());
            return new ClothesDto(
                clothes.getUser().getId(),
                clothes.getId(),
                clothes.getName(),
                clothes.getImageUrl(),
                clothes.getType(),
                attrs
            );
        }

        @Override
        public OotdDto toOotdDto(FeedClothes feedClothes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClothesAttributeWithDefDto toClothesAttributeWithDefDto(ClothesAttribute attribute) {
            throw new UnsupportedOperationException();
        }
    }

    // 테스트용 ClothesAttributeMapper 구현
    static class TestClothesAttributeMapper implements ClothesAttributeMapper {
        @Override
        public ClothesAttributeDto toDto(ClothesAttribute entity) {
            return new ClothesAttributeDto(entity.getDefinition().getId(), entity.getValue());
        }
    }

    @BeforeEach
    void setUp() {
        this.clothesService = new ClothesServiceImpl(
            clothesRepository,
            clothesAttributeRepository,
            new TestClothesMapper(),
            new TestClothesAttributeMapper(),
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
        when(fileStorageService.upload(image)).thenReturn("/uploads/test.png");

        // clothesRepository.save 시 attributes를 실제 엔티티로 연결
        when(clothesRepository.save(any())).thenAnswer(inv -> {
            Clothes c = inv.getArgument(0);
            if (c.getAttributes() != null) {
                List<ClothesAttribute> attrs = c.getAttributes().stream()
                    .map(attr -> ClothesAttribute.create(c, def, attr.getValue()))
                    .toList();
                c.getAttributes().clear();
                c.getAttributes().addAll(attrs);
            }
            return c;
        });

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

    @Test
    void 사용자_의상_목록조회_페이지네이션() {
        // given: 사용자와 의상 엔티티 2개
        UUID ownerId = UUID.randomUUID();
        User user = User.builder().id(ownerId).build();

        Clothes c1 = Clothes.builder()
            .id(UUID.randomUUID())
            .name("티셔츠")
            .type(ClothesType.TOP)
            .user(user)
            .createdAt(Instant.now().minusSeconds(60))
            .build();

        Clothes c2 = Clothes.builder()
            .id(UUID.randomUUID())
            .name("재킷")
            .type(ClothesType.OUTER)
            .user(user)
            .createdAt(Instant.now())
            .build();

        // 최신순 정렬이므로 c2 먼저 나와야 함
        List<Clothes> mockList = List.of(c2);

        when(clothesRepository.findClothesByOwner(ownerId, null, null, null, 1))
            .thenReturn(mockList);
        when(clothesRepository.countByOwner(ownerId, null))
            .thenReturn(2L);

        // when: limit=1로 첫 페이지 요청
        CursorPageResponse<ClothesDto> response = clothesService.getClothesList(
            ownerId, 1, null, null, null
        );

        // then: 1개만 응답 + 다음 페이지 존재
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("재킷");
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isNotBlank();
        assertThat(response.nextIdAfter()).isNotBlank();
        assertThat(response.totalCount()).isEqualTo(2);
    }


    @Test
    void 사용자의_의상_목록_DESC조회_전체타입() {
        // given: 사용자와 의상 엔티티 2개
        UUID ownerId = UUID.randomUUID();
        User user = User.builder().id(ownerId).build();

        Clothes c1 = Clothes.builder()
            .id(UUID.randomUUID())
            .name("티셔츠")
            .type(ClothesType.TOP)
            .user(user)
            .createdAt(Instant.now().minusSeconds(60))
            .build();

        Clothes c2 = Clothes.builder()
            .id(UUID.randomUUID())
            .name("재킷")
            .type(ClothesType.OUTER)
            .user(user)
            .createdAt(Instant.now())
            .build();

        List<Clothes> mockList = List.of(c2, c1);

        when(clothesRepository.findClothesByOwner(ownerId, null, null, null, 10))
            .thenReturn(mockList);
        when(clothesRepository.countByOwner(ownerId, null))
            .thenReturn(2L);

        // when: 서비스 호출
        CursorPageResponse<ClothesDto> response = clothesService.getClothesList(
            ownerId, 10, null, null, null
        );

        // then: 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).name()).isEqualTo("재킷");
        assertThat(response.content().get(1).name()).isEqualTo("티셔츠");
        assertThat(response.hasNext()).isFalse();
        assertThat(response.totalCount()).isEqualTo(2);
    }

    @Test
    void 사용자의_의상_목록_타입필터() {
        // given: TOP 타입 의상 1개
        UUID ownerId = UUID.randomUUID();
        User user = User.builder().id(ownerId).build();

        Clothes top = Clothes.builder()
            .id(UUID.randomUUID())
            .name("티셔츠")
            .type(ClothesType.TOP)
            .user(user)
            .createdAt(Instant.now())
            .build();

        List<Clothes> mockList = List.of(top);

        when(clothesRepository.findClothesByOwner(ownerId, ClothesType.TOP, null, null, 10))
            .thenReturn(mockList);
        when(clothesRepository.countByOwner(ownerId, ClothesType.TOP))
            .thenReturn(1L);

        // when: 서비스 호출
        CursorPageResponse<ClothesDto> response = clothesService.getClothesList(
            ownerId, 10, null, null, ClothesType.TOP
        );

        // then: 응답 검증
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).type()).isEqualTo(ClothesType.TOP);
        assertThat(response.totalCount()).isEqualTo(1);
    }

}