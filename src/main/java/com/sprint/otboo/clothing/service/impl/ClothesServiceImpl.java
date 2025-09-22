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
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.storage.FileStorageService;
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
        log.info("의상 생성 완료: id = {}, ownerId = {}", saved.getId(), saved.getUser().getId());

        return clothesMapper.toDto(saved);
    }

    /**
     * 사용자 의상 목록 조회 (커서 페이지네이션)
     *
     * @param ownerId 조회할 사용자 ID
     * @param limit 조회할 최대 개수
     * @param cursor 마지막 조회 시각
     * @param idAfter 마지막 조회 의상 ID
     * @param type 조회할 의상 타입 (null이면 전체)
     * @return CursorPageResponse<ClothesDto> 페이지네이션 결과
     */
    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ClothesDto> getClothesList(UUID ownerId, int limit, Instant cursor,
        UUID idAfter, ClothesType type
    ) {
        // 유효성 검증
        validateOwnerId(ownerId);
        validateLimit(limit);

        // 필터링
        List<Clothes> clothesList = clothesRepository.findClothesByOwner(ownerId, type, cursor, idAfter, limit);
        long total = clothesRepository.countByOwner(ownerId, type);

        List<ClothesDto> content = clothesList.stream()
            .map(clothesMapper::toDto)
            .toList();

        return buildCursorPageResponse(clothesList, content, total, limit);
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

    // 조회 limit 검증
    private void validateLimit(int limit) {
        if (limit <= 0) throw new ClothesValidationException("조회 개수(limit)는 1 이상이어야 합니다");
    }

    /**
     * 요청 DTO 속성을 ClothesAttribute 엔티티로 변환하여 Clothes 엔티티에 추가
     *
     * @param request 의상 생성 요청 DTO
     * @param clothes 속성을 추가할 Clothes 엔티티
     */
    private void addAttributes(ClothesCreateRequest request, Clothes clothes) {
        if (request.attributes() == null || request.attributes().isEmpty()) return;

        List<ClothesAttribute> attrs = request.attributes().stream()
            // 속성 정의 조회
            .map(dto -> {
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
        log.debug("의상 속성 추가 완료: ownerId={}, attributesCount={}", clothes.getUser().getId(), attrs.size());
    }

    /**
     * CursorPageResponse 생성 헬퍼
     *
     * <p>마지막 요소를 기준으로 다음 페이지 여부 및 커서 설정
     *
     * @param clothesList 조회된 의상 리스트
     * @param content DTO 변환된 의상 리스트
     * @param total 전체 의상 개수
     * @param limit 조회 개수 제한
     * @return CursorPageResponse<ClothesDto>
     */
    private CursorPageResponse<ClothesDto> buildCursorPageResponse(List<Clothes> clothesList, List<ClothesDto> content, long total, int limit
    ) {
        Instant nextCursor = null;
        UUID nextIdAfter = null;
        boolean hasNext = false;

        if (!clothesList.isEmpty()) {
            // 마지막 요소 기준으로 다음 페이지 존재 여부 계산
            Clothes last = clothesList.get(clothesList.size() - 1);
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
}