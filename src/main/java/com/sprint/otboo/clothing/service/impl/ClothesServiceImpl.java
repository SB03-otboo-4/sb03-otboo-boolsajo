package com.sprint.otboo.clothing.service.impl;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.mapper.ClothesAttributeMapper;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.repository.ClothesAttributeRepository;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.clothing.service.ClothesService;
import com.sprint.otboo.clothing.storage.FileStorageService;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * ClothesService 구현체
 *
 * <p>의상(Clothes) 관련 비즈니스 로직을 수행
 *
 * <ul>
 *   <li>요청 데이터 검증</li>
 *   <li>이미지 업로드 및 URL 생성</li>
 *   <li>Clothes 엔티티 저장</li>
 *   <li>ClothesAttribute 엔티티 저장</li>
 *   <li>DTO 변환 및 반환</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesServiceImpl implements ClothesService {

    private final ClothesRepository clothesRepository;
    private final ClothesAttributeRepository clothesAttributeRepository;
    private final ClothesMapper clothesMapper;
    private final ClothesAttributeMapper clothesAttributeMapper;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final ClothesAttributeDefRepository defRepository;

    /**
     * 새로운 의상을 생성합니다.
     *
     * <p>절차:
     * <ol>
     *   <li>요청 데이터 검증</li>
     *   <li>이미지 업로드 및 URL 생성</li>
     *   <li>Clothes 엔티티 저장</li>
     *   <li>ClothesAttribute 엔티티 저장</li>
     *   <li>DTO 변환 및 반환</li>
     * </ol>
     *
     * @param request 의상 생성 요청 DTO
     * @param image 업로드할 이미지 파일 (선택)
     * @return 저장 완료된 ClothesDto
     * @throws ClothesValidationException 요청 데이터가 유효하지 않을 경우 발생
     */
    @Override
    @Transactional
    public ClothesDto createClothes(ClothesCreateRequest request, MultipartFile image) {

        // 요청 유효성 검증
        validateRequest(request);

        String imageUrl = fileStorageService.upload(image);

        var user = userRepository.findById(request.ownerId())
            .orElseThrow(() -> new ClothesValidationException("유효하지 않은 사용자"));

        Clothes clothes = Clothes.builder()
            .user(user)
            .name(request.name())
            .imageUrl(imageUrl)
            .type(request.type())
            .build();

        // Clothes 엔티티에 속성 추가
        addAttributes(request, clothes);

        Clothes saved = clothesRepository.save(clothes);

        return clothesMapper.toDto(saved);
    }

    @Override
    public CursorPageResponse<ClothesDto> getClothesList(UUID ownerId, int limit, Instant cursor,
        UUID idAfter, ClothesType type
    ) {
        List<Clothes> clothesList = clothesRepository
            .findClothesByOwner(ownerId, type, cursor, idAfter, limit);

        long total = clothesRepository.countByOwner(ownerId, type);

        List<ClothesDto> content = clothesList.stream()
            .map(clothesMapper::toDto)
            .toList();

        Instant nextCursor = null;
        UUID nextIdAfter = null;
        boolean hasNext = false;

        if (!clothesList.isEmpty()) {
            Clothes last =  clothesList.get(clothesList.size() - 1);
            nextCursor = last.getCreatedAt();
            nextIdAfter = last.getId();
            hasNext = clothesList.size() == limit && total > limit;
        }

        return new CursorPageResponse<>(
            content,
            nextCursor != null ? nextCursor.toString() : null,
            nextIdAfter != null ? nextIdAfter.toString() : null,
            hasNext,
            total,
            "createdAt",
            "DESCENDING"
        );
    }

    /**
     * 요청 기본 검증
     *
     * @param request 의상 생성 요청 DTO
     * @throws ClothesValidationException 필수 값이 누락되거나 유효하지 않은 경우
     */
    private void validateRequest(ClothesCreateRequest request) {
        if (request == null) throw new ClothesValidationException("요청 데이터가 존재하지 않음");
        validateOwnerId(request.ownerId());
        validateName(request.name());
        validateType(request.type());

        log.debug("의상 등록 요청 검증 완료 : ownerId = {}, name = {}", request.ownerId(), request.name());
    }

    // ownerId 검증
    private void validateOwnerId(UUID ownerId) {
        if (ownerId == null) throw new ClothesValidationException("의상 소유자의 ID가 필요합니다");
    }

    // name 검증
    private void validateName(String name) {
        if (name == null || name.isBlank()) throw new ClothesValidationException("의상 이름은 필수입니다");
    }

    // type 검증
    private void validateType(ClothesType type) {
        if (type == null) throw new ClothesValidationException("의상 타입은 필수입니다");
    }

    /**
     * 요청 DTO에 포함된 속성을 ClothesAttribute 엔티티로 변환하여
     * 해당 Clothes 엔티티에 추가합니다.
     *
     * @param request 의상 생성 요청 DTO
     * @param clothes 속성을 추가할 Clothes 엔티티
     * @throws ClothesValidationException 유효하지 않은 속성 정의 ID가 있을 경우
     */
    private void addAttributes(ClothesCreateRequest request, Clothes clothes) {
        if (request.attributes() == null || request.attributes().isEmpty()) {
            return;
        }

        List<ClothesAttribute> attrs = request.attributes().stream()
            .map(dto -> {
                // 속성 정의 조회
                var def = defRepository.findById(dto.definitionId())
                    .orElseThrow(() -> new ClothesValidationException("유효하지 않은 속성 정의"));

                return ClothesAttribute.builder()
                    .clothes(clothes)
                    .definition(def)
                    .value(dto.value())
                    .build();
            })
                .collect(Collectors.toList());

        clothes.getAttributes().addAll(attrs);
    }
}