package com.sprint.otboo.clothing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeWithDefDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesUpdateRequest;
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
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.storage.FileStorageService;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.feed.entity.FeedClothes;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
@DisplayName("의상 서비스 테스트")
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
    void 사용자_의상_목록_조회_최신순_정렬_검증() {
        // given: 사용자와 의상 엔티티 5개
        UUID ownerId = UUID.randomUUID();
        User user = User.builder().id(ownerId).build();

        Instant now = Instant.now();
        Clothes c1 = Clothes.builder().id(UUID.randomUUID()).name("티셔츠").type(ClothesType.TOP).user(user).createdAt(now.minusSeconds(300)).build();
        Clothes c2 = Clothes.builder().id(UUID.randomUUID()).name("재킷").type(ClothesType.OUTER).user(user).createdAt(now.minusSeconds(200)).build();
        Clothes c3 = Clothes.builder().id(UUID.randomUUID()).name("바지").type(ClothesType.BOTTOM).user(user).createdAt(now.minusSeconds(100)).build();
        Clothes c4 = Clothes.builder().id(UUID.randomUUID()).name("모자").type(ClothesType.HAT).user(user).createdAt(now.minusSeconds(50)).build();
        Clothes c5 = Clothes.builder().id(UUID.randomUUID()).name("신발").type(ClothesType.SHOES).user(user).createdAt(now).build();

        // 최신순 정렬
        List<Clothes> mockList = List.of(c5, c4, c3, c2, c1);

        when(clothesRepository.findClothesByOwner(ownerId, null, null, null, 10))
            .thenReturn(mockList);
        when(clothesRepository.countByOwner(ownerId, null))
            .thenReturn(5L);

        // when: 서비스 메서드 호출
        CursorPageResponse<ClothesDto> response = clothesService.getClothesList(ownerId, 10, null, null, null);

        // then: createdAt 기준 최신순 정렬 검증
        List<String> expectedOrder = List.of("신발", "모자", "바지", "재킷", "티셔츠");
        List<String> actualOrder = response.data().stream()
            .map(ClothesDto::name)
            .collect(Collectors.toList());

        assertThat(actualOrder).isEqualTo(expectedOrder);
        assertThat(response.data()).hasSize(5);
        assertThat(response.totalCount()).isEqualTo(5);
    }

    @Test
    void 사용자의_의상_목록_타입필터_검증() {
        // given: 각 타입별 의상 1개씩 + 필터 대상
        UUID ownerId = UUID.randomUUID();
        User user = User.builder().id(ownerId).build();

        Clothes top = Clothes.builder()
            .id(UUID.randomUUID())
            .name("티셔츠")
            .type(ClothesType.TOP)
            .user(user)
            .createdAt(Instant.now())
            .build();

        Clothes outer = Clothes.builder()
            .id(UUID.randomUUID())
            .name("재킷")
            .type(ClothesType.OUTER)
            .user(user)
            .createdAt(Instant.now())
            .build();

        Clothes shoes = Clothes.builder()
            .id(UUID.randomUUID())
            .name("운동화")
            .type(ClothesType.SHOES)
            .user(user)
            .createdAt(Instant.now())
            .build();

        List<Clothes> allClothes = List.of(top, outer, shoes);

        // Repository mocking: TOP 타입만 반환
        when(clothesRepository.findClothesByOwner(ownerId, ClothesType.TOP, null, null, 10))
            .thenReturn(List.of(top));
        when(clothesRepository.countByOwner(ownerId, ClothesType.TOP))
            .thenReturn(1L);

        // when: TOP 타입 조회
        CursorPageResponse<ClothesDto> topRes = clothesService.getClothesList(
            ownerId, 10, null, null, ClothesType.TOP
        );

        // then: TOP 타입만 포함되고, 다른 타입은 제외
        assertThat(topRes.data()).hasSize(1);
        assertThat(topRes.data().get(0).type()).isEqualTo(ClothesType.TOP);
        assertThat(topRes.data().stream().anyMatch(c -> c.type() != ClothesType.TOP)).isFalse();
        assertThat(topRes.totalCount()).isEqualTo(1);
    }


    @Test
    void 사용자_의상_목록_조회_커서페이지네이션() {
        // given: 사용자와 의상 엔티티 5개
        UUID ownerId = UUID.randomUUID();
        User user = User.builder().id(ownerId).build();

        Instant now = Instant.now();
        Clothes c1 = Clothes.builder().id(UUID.randomUUID()).name("티셔츠").type(ClothesType.TOP).user(user).createdAt(now.minusSeconds(300)).build();
        Clothes c2 = Clothes.builder().id(UUID.randomUUID()).name("재킷").type(ClothesType.OUTER).user(user).createdAt(now.minusSeconds(200)).build();
        Clothes c3 = Clothes.builder().id(UUID.randomUUID()).name("바지").type(ClothesType.BOTTOM).user(user).createdAt(now.minusSeconds(100)).build();
        Clothes c4 = Clothes.builder().id(UUID.randomUUID()).name("모자").type(ClothesType.HAT).user(user).createdAt(now.minusSeconds(50)).build();
        Clothes c5 = Clothes.builder().id(UUID.randomUUID()).name("신발").type(ClothesType.SHOES).user(user).createdAt(now).build();

        // 첫 페이지에서 limit=2
        List<Clothes> firstPage = List.of(c5, c4);
        when(clothesRepository.findClothesByOwner(ownerId, null, null, null, 2))
            .thenReturn(firstPage);
        when(clothesRepository.countByOwner(ownerId, null))
            .thenReturn(5L);

        // 첫 페이지 호출
        CursorPageResponse<ClothesDto> firstResponse = clothesService.getClothesList(ownerId, 2, null, null, null);

        // 다음 페이지 조회를 위한 cursor와 idAfter
        Instant nextCursor = Instant.parse(firstResponse.nextCursor());
        UUID nextIdAfter = UUID.fromString(firstResponse.nextIdAfter());

        // 두 번째 페이지: limit=2
        List<Clothes> secondPage = List.of(c3, c2);
        when(clothesRepository.findClothesByOwner(ownerId, null, nextCursor, nextIdAfter, 2))
            .thenReturn(secondPage);

        // when: 두 번째 페이지 호출
        CursorPageResponse<ClothesDto> secondResponse = clothesService.getClothesList(ownerId, 2, nextCursor, nextIdAfter, null);

        // then: 두 번째 페이지 내용 검증
        List<String> expectedNames = List.of("바지", "재킷");
        List<String> actualNames = secondResponse.data().stream().map(ClothesDto::name).toList();

        assertThat(actualNames).isEqualTo(expectedNames);
        assertThat(secondResponse.data()).hasSize(2);
        assertThat(secondResponse.hasNext()).isTrue(); // 마지막 페이지가 아니므로 hasNext = true
        assertThat(secondResponse.totalCount()).isEqualTo(5);
    }

    @Test
    void 사용자_의상_목록_타입필터_커서페이지네이션_검증() {
        // given: 사용자와 의상 엔티티 4개
        UUID ownerId = UUID.randomUUID();
        User user = User.builder().id(ownerId).build();

        Instant now = Instant.now();
        Clothes top1 = Clothes.builder().id(UUID.randomUUID()).name("티셔츠").type(ClothesType.TOP).user(user).createdAt(now.minusSeconds(300)).build();
        Clothes top2 = Clothes.builder().id(UUID.randomUUID()).name("셔츠").type(ClothesType.TOP).user(user).createdAt(now.minusSeconds(200)).build();
        Clothes outer = Clothes.builder().id(UUID.randomUUID()).name("재킷").type(ClothesType.OUTER).user(user).createdAt(now.minusSeconds(150)).build();
        Clothes shoes = Clothes.builder().id(UUID.randomUUID()).name("운동화").type(ClothesType.SHOES).user(user).createdAt(now.minusSeconds(100)).build();

        // 전체 TOP 타입 리스트: 최신순
        List<Clothes> topClothes = List.of(top2, top1);

        // 첫 페이지 limit=2
        List<Clothes> firstPage = List.of(top2, top1);
        when(clothesRepository.findClothesByOwner(ownerId, ClothesType.TOP, null, null, 2))
            .thenReturn(firstPage);
        when(clothesRepository.countByOwner(ownerId, ClothesType.TOP))
            .thenReturn(2L);

        // when: 첫 페이지 조회
        CursorPageResponse<ClothesDto> firstResponse = clothesService.getClothesList(ownerId, 2, null, null, ClothesType.TOP);

        // then: 첫 페이지 검증
        assertThat(firstResponse.data()).hasSize(2);
        assertThat(firstResponse.data().stream().allMatch(c -> c.type() == ClothesType.TOP)).isTrue();
        assertThat(firstResponse.hasNext()).isFalse(); // 총 2개라서 다음 페이지 없음
        assertThat(firstResponse.totalCount()).isEqualTo(2);

        // 순서 검증: 최신순
        List<String> expectedOrder = List.of("셔츠", "티셔츠");
        List<String> actualOrder = firstResponse.data().stream()
            .map(ClothesDto::name)
            .toList();
        assertThat(actualOrder).isEqualTo(expectedOrder);
    }

    @Test
    void 의상_수정_이름_타입_이미지_속성_갱신() throws IOException {
        // given: 의상, 사용자, 속성 생성
        UUID ownerId = UUID.randomUUID();
        UUID clothesId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();

        User user = User.builder().id(ownerId).build();
        ClothesAttributeDef def = ClothesAttributeDef.builder().id(defId).name("색상").build();

        ClothesAttribute existingAttr = ClothesAttribute.create(null, def, "White");
        Clothes existing = Clothes.builder()
            .id(clothesId)
            .user(user)
            .name("기존 티셔츠")
            .type(ClothesType.TOP)
            .imageUrl("/uploads/old.png")
            .attributes(new ArrayList<>(List.of(existingAttr)))
            .build();

        List<ClothesAttribute> linkedAttributes = existing.getAttributes().stream()
            .map(attr -> ClothesAttribute.create(existing, attr.getDefinition(), attr.getValue()))
            .toList();
        existing.getAttributes().clear();
        existing.getAttributes().addAll(linkedAttributes);

        ClothesUpdateRequest request = new ClothesUpdateRequest(
            "새 티셔츠",
            ClothesType.TOP,
            List.of(new ClothesAttributeDto(defId, "Black"))
        );

        MultipartFile newImage = mock(MultipartFile.class);
        when(fileStorageService.upload(newImage)).thenReturn("/uploads/new.png");
        when(clothesRepository.findById(clothesId)).thenReturn(Optional.of(existing));
        when(defRepository.findById(defId)).thenReturn(Optional.of(def));
        when(clothesRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when: 실제 테스트 대상 동작 수행
        ClothesDto result = clothesService.updateClothes(clothesId, request, newImage);

        // then: 동작 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("새 티셔츠");
        assertThat(result.type()).isEqualTo(ClothesType.TOP);
        assertThat(result.imageUrl()).isEqualTo("/uploads/new.png");
        assertThat(result.attributes()).hasSize(1);
        assertThat(result.attributes().get(0).definitionId()).isEqualTo(defId);
        assertThat(result.attributes().get(0).value()).isEqualTo("Black");

        verify(clothesRepository, times(1)).save(any());
        verify(fileStorageService, times(1)).upload(newImage);
    }

    @Test
    void 의상_수정_이미지_없이() throws Exception {
        // given: 테스트 준비
        UUID clothesId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).build();

        Clothes existing = Clothes.builder()
            .id(clothesId)
            .user(user)
            .name("기존 상의")
            .type(ClothesType.TOP)
            .imageUrl("old_image_url")
            .attributes(new ArrayList<>())
            .build();

        when(clothesRepository.findById(clothesId)).thenReturn(Optional.of(existing));

        ClothesAttributeDef def = ClothesAttributeDef.builder().id(UUID.randomUUID()).build();
        when(defRepository.findById(any())).thenReturn(Optional.of(def));
        when(clothesRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClothesUpdateRequest request = new ClothesUpdateRequest(
            "새 상의",
            ClothesType.OUTER,
            List.of(new ClothesAttributeDto(def.getId(), "BLUE"))
        );

        // when: 의상 업데이트 수행 (이미지 없이)
        ClothesDto updated = clothesService.updateClothes(clothesId, request, null);

        // then: 검증
        assertThat(updated.name()).isEqualTo("새 상의");
        assertThat(updated.type()).isEqualTo(ClothesType.OUTER);
        assertThat(updated.imageUrl()).isEqualTo("old_image_url");
        assertThat(updated.attributes()).hasSize(1);
        assertThat(updated.attributes().get(0).value()).isEqualTo("BLUE");
        assertThat(updated.attributes().get(0).definitionId()).isEqualTo(def.getId());

        verify(clothesRepository).save(any(Clothes.class));
    }

    @Test
    void 의상_수정_존재하지_않는_의상() {
        // given: 없는 의상 ID
        UUID clothesId = UUID.randomUUID();
        when(clothesRepository.findById(clothesId)).thenReturn(Optional.empty());

        ClothesUpdateRequest request = new ClothesUpdateRequest(
            "새 상의",
            ClothesType.TOP,
            Collections.emptyList()
        );

        // when & then: 예외 발생 확인
        assertThatThrownBy(() -> clothesService.updateClothes(clothesId, request, null))
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CLOTHES_NOT_FOUND);
    }

    @Test
    void 의상_삭제_성공() {
        // given: 존재하는 의상 ID와 엔티티 준비
        UUID clothesId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).build();
        Clothes clothes = Clothes.builder()
            .id(clothesId)
            .name("삭제할 티셔츠")
            .type(ClothesType.TOP)
            .user(user)
            .build();

        when(clothesRepository.findById(clothesId)).thenReturn(Optional.of(clothes));
        doNothing().when(clothesRepository).delete(clothes);

        // when: 서비스 메서드 호출
        clothesService.deleteClothes(clothesId);

        // then: delete 메서드가 정확히 한 번 호출되었는지 검증
        verify(clothesRepository, times(1)).delete(clothes);
    }

    @Test
    void 의상_삭제_실패_존재하지않는의상() {
        // given: 존재하지 않는 ID
        UUID clothesId = UUID.randomUUID();
        when(clothesRepository.findById(clothesId)).thenReturn(Optional.empty());

        // when & then: CustomException 발생 검증
        assertThatThrownBy(() -> clothesService.deleteClothes(clothesId))
            .isInstanceOf(CustomException.class)
            .hasMessage("의상 정보를 찾을 수 없습니다.");
    }
}