package com.sprint.otboo.clothing.service;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.mapper.ClothesAttributeMapper;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeRepository;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ClothesService 구현체
 *
 * <p>Clothes 관련 비즈니스 로직 수행
 *
 * <ul>
 *   <li>사용자 요청 검증</li>
 *   <li>Clothes 저장</li>
 *   <li>ClothesAttribute 저장</li>
 *   <li>최종 ClothesDto 변환</li>
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

    /**
     * 의상 생성 처리
     *
     * @param request 의상 생성 요청 DTO
     * @return 저장 완료된 ClothesDto
     */
    @Override
    @Transactional
    public ClothesDto createClothes(ClothesCreateRequest request) {

        // 요청 유효성 검증
        validateRequest(request);

        // Clothes Entity 저장
        Clothes saved = saveClothes(request);

        // ClothesAttribute Entity 저장
        List<ClothesAttribute> attrs = saveAttributes(request, saved.getId());

        // DTO 변환 후 반환
        return toDtoResult(saved, attrs);
    }


    /**
     * 요청 기본 검증
     *
     * @param request 의상 생성 요청 DTO
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
     * 의상 저장
     *
     * @param request 의상 생성 요청 DTO
     * @return 저장된 Clothes 엔티티
     */
    private Clothes saveClothes(ClothesCreateRequest request) {
        // Create Entity
        Clothes clothes = Clothes.create(
            request.ownerId(),
            request.name(),
            "",
            request.type()
        );

        // Save DB
        Clothes saved = clothesRepository.save(clothes);
        log.info("Clothes 저장 완료 : id = {}, name = {}", saved.getId(), saved.getName());

        return saved;
    }

    /**
     * 의상 속성 저장
     *
     * @param request 의상 생성 요청 DTO
     * @param clothesId 저장된 의상 ID
     * @return 저장된 의상 속성 리스트
     */
    private List<ClothesAttribute> saveAttributes(ClothesCreateRequest request, UUID clothesId) {
        if (request.attributes() == null || request.attributes().isEmpty()) {
            log.debug("저장할 의상 속성 없음");
            return List.of();
        }

        // DTO -> 엔티티 변환
        List<ClothesAttribute> attrs = request.attributes()
            .stream()
            .map(dto -> clothesAttributeMapper.toEntity(dto, clothesId))
            .toList();

        // DB 저장
        clothesAttributeRepository.saveAll(attrs);
        log.info("의상 속성 저장 완료 : count = {}", attrs.size());

        return attrs;
    }

    /**
     * 저장된 Clothes 및 ClothesAttribute 엔티티를 DTO로 변환
     *
     * @param saved 저장된 의상 엔티티
     * @param attrs 저장된 의상 속성 리스트
     * @return 변환된 ClothesDto
     */
    private ClothesDto toDtoResult(Clothes saved, List<ClothesAttribute> attrs) {
        // 속성 DTO 변환
        var attrDtos = attrs.stream()
            .map(clothesAttributeMapper::toDto)
            .toList();

        // 의상 DTO 변환
        ClothesDto dto = clothesMapper.toDto(saved, attrDtos);
        log.debug("ClothesDto 변환 완료 : id = {}", dto.id());

        return dto;
    }
}