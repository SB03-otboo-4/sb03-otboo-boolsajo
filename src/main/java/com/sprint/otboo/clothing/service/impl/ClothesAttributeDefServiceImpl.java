package com.sprint.otboo.clothing.service.impl;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.service.ClothesAttributeDefService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 의상 속성 정의 서비스 구현체
 *
 * <p>ADMIN이 관리하는 의상 속성 정의(CLOTHES ATTRIBUTE DEF)를 생성, 검증, 저장하는 비즈니스 로직 수행
 *
 * <ul>
 *   <li>요청 데이터 검증</li>
 *   <li>선택값 리스트를 문자열로 변환</li>
 *   <li>ClothesAttributeDef 엔티티 생성 및 저장</li>
 *   <li>DTO 변환 및 반환</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAttributeDefServiceImpl implements ClothesAttributeDefService {

    private final ClothesAttributeDefRepository clothesAttributeDefRepository;
    private final ClothesMapper  clothesMapper;

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