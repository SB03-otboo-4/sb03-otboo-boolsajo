package com.sprint.otboo.clothing.service.impl;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefUpdateRequest;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.mapper.ClothesAttributeDefMapper;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.service.ClothesAttributeDefService;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 의상 속성 정의 서비스 구현체
 *
 * <p>ADMIN이 관리하는 의상 속성 정의를 생성, 수정, 조회하는 비즈니스 로직 수행
 *
 * <ul>
 *   <li>요청 검증 및 선택값 문자열 변환</li>
 *   <li>ClothesAttributeDef 엔티티 생성/수정/조회</li>
 *   <li>DTO 변환 후 반환</li>
 *   <li>정렬 기준과 키워드 필터를 적용한 목록 조회</li>
 * </ul>
 *
 * <p>예외: 잘못된 입력 시 ClothesValidationException, 허용되지 않는 정렬 기준 시 CustomException 발생
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAttributeDefServiceImpl implements ClothesAttributeDefService {

    private final ClothesAttributeDefRepository clothesAttributeDefRepository;
    private final ClothesAttributeDefMapper clothesAttributeDefMapper;
    private final ClothesMapper clothesMapper;

    /**
     * 새로운 의상 속성 정의 등록
     *
     * <p>절차:
     * <ol>
     *   <li>요청 검증</li>
     *   <li>선택값 문자열 변환</li>
     *   <li>ClothesAttributeDef 엔티티 생성 및 저장</li>
     *   <li>DTO 변환 및 반환</li>
     * </ol>
     *
     * @param request 의상 속성 정의 생성 요청 DTO
     * @return 생성된 의상 속성 정의 DTO
     * @throws ClothesValidationException 요청이 유효하지 않을 경우 발생
     */
    @Transactional
    @Override
    public ClothesAttributeDefDto createAttributeDef(ClothesAttributeDefCreateRequest request) {

        // 요청 검증
        validateRequest(request);

        // 선택값 문자열 변환
        String selectValues = convertSelectableValues(request.selectableValues());

        // 엔티티 생성
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .name(request.name())
            .selectValues(selectValues)
            .build();

        // 저장
        ClothesAttributeDef saved = clothesAttributeDefRepository.save(def);
        log.info("의상 속성 정의 등록 완료 : id = {}, name = {}", saved.getId(), saved.getName());

        // DTO 변환 및 반환
        return clothesMapper.toClothesAttributeDefDto(saved);
    }

    /**
     * 기존 의상 속성 정의 수정
     *
     * <p>요청 검증 후 엔티티 조회, update 헬퍼 메서드 호출 후 저장 및 DTO 변환.
     *
     * @param id 수정 대상 의상 속성 정의 ID
     * @param request 수정 요청 DTO
     * @return 수정된 의상 속성 정의 DTO
     * @throws ClothesValidationException 요청 데이터가 없거나 필수 값이 누락되거나 존재하지 않는 정의 ID일 때 발생
     */
    @Transactional
    @Override
    public ClothesAttributeDefDto updateAttributeDef(UUID id, ClothesAttributeDefUpdateRequest request) {
        log.info("의상 속성 정의 수정 요청 - id: {}, request: {}", id, request);

        validateUpdateRequest(request);

        ClothesAttributeDef def = clothesAttributeDefRepository.findById(id)
            .orElseThrow(() ->
                new ClothesValidationException("존재하지 않는 의상 속성 정의"));
        log.debug("의상 속성 정의 조회 성공 - id: {}", id);

        // 엔티티의 update 헬퍼 메서드 활용
        def.update(request.name(), convertSelectableValues(request.selectableValues()));

        ClothesAttributeDef updated = clothesAttributeDefRepository.save(def);
        log.info("의상 속성 정의 저장 완료 - id: {}", id);

        return clothesMapper.toClothesAttributeDefDto(updated);
    }

    /**
     * 모든 의상 속성 정의 목록을 조회하고 DTO로 변환하여 반환합니다.
     *
     * <p>정렬 기준(sortBy)과 정렬 방향(sortDirection), 검색 키워드(keywordLike)를 동적으로 적용할 수 있습니다.
     *
     * <ul>
     *   <li>sortBy : "name" 또는 "createdAt" (null이면 기본 "name")</li>
     *   <li>sortDirection : "ASCENDING" 또는 "DESCENDING" (null이면 기본 ASC)</li>
     *   <li>keywordLike : 이름에 포함된 키워드로 필터링 (null 또는 빈 문자열이면 전체 조회)</li>
     * </ul>
     *
     * @param sortBy 정렬 기준 컬럼 ("name" 또는 "createdAt")
     * @param sortDirection 정렬 방향 ("ASCENDING" 또는 "DESCENDING")
     * @param keywordLike 이름 검색 키워드
     * @return 의상 속성 정의 DTO 목록
     * @throws CustomException sortBy 값이 허용되지 않는 경우 INVALID_SORT_BY 예외 발생
     */
    @Transactional(readOnly = true)
    @Override
    public List<ClothesAttributeDefDto> listAttributeDefs(String sortBy, String sortDirection, String keywordLike
    ) {
        // 입력 파라미터 로깅
        log.info("의상 속성 정의 조회 호출 - sortBy: {}, sortDirection: {}, keywordLike: {}", sortBy, sortDirection, keywordLike);

        // 정렬 기준
        Direction direction = "DESCENDING".equalsIgnoreCase(sortDirection)
            ? Direction.DESC
            : Direction.ASC;

        // 정렬 속성
        String sortProperty = switch (sortBy != null ? sortBy : "name") {
            case "createdAt" -> "createdAt";
            case "name" -> "name";
            default -> throw new CustomException(ErrorCode.INVALID_SORT_BY);
        };

        Sort sort = Sort.by(direction, sortProperty);

        // 키워드 필터 적용
        List<ClothesAttributeDef> entities = (keywordLike == null || keywordLike.isBlank())
            ? clothesAttributeDefRepository.findAll(sort)
            : clothesAttributeDefRepository.findByNameContainingIgnoreCase(keywordLike, sort);
        log.info("조회된 의상 속성 정의 개수: {}", entities.size());

        // DTO 변환
        return entities.stream()
            .map(clothesAttributeDefMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * 요청 검증
     *
     * @param request 생성 요청 DTO
     * @throws ClothesValidationException 이름이 없을 경우 발생
     */
    private void validateRequest(ClothesAttributeDefCreateRequest request) {
        if (request == null) {
            throw new ClothesValidationException("요청 데이터가 존재하지 않습니다");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ClothesValidationException("속성 이름은 필수입니다");
        }
        log.debug("속성 정의 요청 검증 완료: name={}", request.name());
    }

    /**
     * 의상 속성 정의 수정 요청 검증
     *
     * <p>속성 이름과 선택값 리스트 존재 여부 확인
     *
     * @param request 수정 요청 DTO
     * @throws ClothesValidationException 필수 값 누락 시 발생
     */
    private void validateUpdateRequest(ClothesAttributeDefUpdateRequest request) {
        if (request == null) {
            throw new ClothesValidationException("요청 데이터가 존재하지 않습니다");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ClothesValidationException("속성 이름은 필수입니다");
        }
        if (request.selectableValues() == null || request.selectableValues().isEmpty()) {
            throw new ClothesValidationException("속성 값은 최소 1개 이상 필요합니다");
        }
    }

    /**
     * 선택값 리스트를 콤마로 구분된 문자열로 변환
     *
     * @param selectableValues 선택값 리스트
     * @return 콤마로 구분된 문자열 (값이 없으면 null)
     */
    private String convertSelectableValues(List<String> selectableValues) {
        if (selectableValues == null || selectableValues.isEmpty()) {
            return null;
        }
        return String.join(",", selectableValues);
    }
}