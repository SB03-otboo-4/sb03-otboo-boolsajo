package com.sprint.otboo.clothing.controller;

import com.sprint.otboo.clothing.controller.api.ClothesAttributeDefApi;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefUpdateRequest;
import com.sprint.otboo.clothing.service.ClothesAttributeDefService;
import com.sprint.otboo.common.exception.CustomException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 의상 속성 정의 API 컨트롤러
 *
 * <p>ADMIN 권한만 접근 가능하며, 의상 속성 정의 생성/수정/삭제 기능 제공,
 * 속성 조회는 공용 기능
 */
@Slf4j
@RestController
@RequestMapping("/api/clothes/attribute-defs")
@RequiredArgsConstructor
public class ClothesAttributeDefController implements ClothesAttributeDefApi {

    private final ClothesAttributeDefService clothesAttributeDefService;

    /**
     * 의상 속성 정의 등록
     *
     * <p>ADMIN 권한이 있어야 접근 가능.
     *
     * @param request 의상 속성 정의 생성 요청 DTO
     * @return 생성된 의상 속성 정의 DTO
     */
    @Override
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClothesAttributeDefDto> createAttributeDef(@RequestBody ClothesAttributeDefCreateRequest request) {
        log.info("의상 속성 정의 등록 요청 : name = {}", request.name());
        ClothesAttributeDefDto result = clothesAttributeDefService.createAttributeDef(request);

        log.info("의상 속성 정의 등록 완료 : id = {}, name = {}", result.id(), result.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * 의상 속성 정의 수정
     *
     * @param definitionId 수정 대상 의상 속성 정의 ID
     * @param request 수정 요청 DTO
     * @return 수정된 의상 속성 정의 DTO
     */
    @Override
    @PatchMapping("/{definitionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClothesAttributeDefDto> updateAttributeDef(
        @PathVariable("definitionId") UUID definitionId,
        @RequestBody ClothesAttributeDefUpdateRequest request
    ) {
        log.info("의상 속성 정의 수정 요청 - id: {}, request: {}", definitionId, request);
        ClothesAttributeDefDto updatedDto = clothesAttributeDefService.updateAttributeDef(definitionId, request);

        log.info("의상 속성 정의 수정 완료 - id: {}, updated name: {}, updated values: {}",
            updatedDto.id(), updatedDto.name(), updatedDto.selectableValues());
        return ResponseEntity.ok(updatedDto);
    }

    /**
     * 의상 속성 정의 목록 조회
     *
     * @param sortBy 정렬 기준 ("name" 또는 "createdAt")
     * @param sortDirection 정렬 방향 ("ASCENDING" 또는 "DESCENDING")
     * @param keywordLike 이름 검색 키워드 (옵션)
     * @return 의상 속성 정의 DTO 리스트
     */
    @Override
    @GetMapping
    public ResponseEntity<List<ClothesAttributeDefDto>> getAttributeDefs(
        @RequestParam String sortBy,
        @RequestParam String sortDirection,
        @RequestParam(required = false) String keywordLike
    ) {
        // 요청 파라미터 로깅
        log.info("의상 속성 정의 목록 조회 요청 - sortBy: {}, sortDirection: {}, keywordLike: {}",
            sortBy, sortDirection, keywordLike);

        List<ClothesAttributeDefDto> result = clothesAttributeDefService.listAttributeDefs(sortBy, sortDirection, keywordLike);
        log.info("조회 완료 - 결과 개수: {}", result.size());

        return ResponseEntity.ok(result);
    }

    /**
     * 의상 속성 정의 삭제
     *
     * @param definitionId 삭제할 의상 속성 정의 ID
     * @return 삭제 성공 시 204 No Content
     * @throws CustomException 삭제할 정의가 존재하지 않으면 RESOURCE_NOT_FOUND 예외 발생
     */
    @Override
    @DeleteMapping("/{definitionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAttributeDef(@PathVariable UUID definitionId) {
        log.info("의상 속성 정의 삭제 요청 - id: {}", definitionId);

        clothesAttributeDefService.deleteAttributeDef(definitionId);

        log.info("의상 속성 정의 삭제 완료 - id: {}", definitionId);
        return ResponseEntity.noContent().build();
    }
}