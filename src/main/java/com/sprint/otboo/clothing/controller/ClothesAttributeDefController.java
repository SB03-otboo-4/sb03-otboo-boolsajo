package com.sprint.otboo.clothing.controller;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefUpdateRequest;
import com.sprint.otboo.clothing.service.ClothesAttributeDefService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 의상 속성 정의 API 컨트롤러
 *
 * <p>ADMIN 권한만 접근 가능하며, 의상 속성 정의 생성/수정/삭제 기능 제공.
 */
@Slf4j
@RestController
@RequestMapping("/api/clothes/attribute-defs")
@RequiredArgsConstructor
public class ClothesAttributeDefController {

    private final ClothesAttributeDefService clothesAttributeDefService;

    /**
     * 의상 속성 정의 등록
     *
     * <p>ADMIN 권한이 있어야 접근 가능.
     *
     * @param request 의상 속성 정의 생성 요청 DTO
     * @return 생성된 의상 속성 정의 DTO
     */
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
}