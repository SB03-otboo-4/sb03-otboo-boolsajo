package com.sprint.otboo.clothing.controller;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesUpdateRequest;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.service.ClothesService;
import com.sprint.otboo.clothing.valid.ClothesTypeValid;
import com.sprint.otboo.common.dto.CursorPageResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Clothes Controller
 * <p>의상 관련 REST API 요청을 처리하는 컨트롤러</p>
 *
 * <ul>
 *   <li>의상 등록</li>
 *   <li>의상 목록 조회</li>
 *   <li>의상 수정</li>
 *   <li>의상 삭제</li>
 * </ul>
 *
 * <p>보안: 인증된 사용자( USER, ADMIN )만 접근 가능하도록 Spring Security 설정</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/clothes")
@RequiredArgsConstructor
public class ClothesController {

    private final ClothesService clothesService;

    /**
     * 의상 등록
     *
     * <p>multipart/form-data 요청을 처리하며, 의상 정보를 등록하고 선택적으로 이미지 파일을 함께 저장</p>
     *
     * @param request 등록할 의상 정보 DTO
     * @param image 업로드할 이미지 파일 (선택)
     * @return {@link ResponseEntity}<{@link ClothesDto}> 생성된 의상 정보와 HTTP 상태 코드
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothesDto> createClothes(
        @RequestPart("request") ClothesCreateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        log.info("POST /api/clothes 요청 수신: ownerId={}, name={}, image={}",
            request.ownerId(), request.name(), image != null ? image.getOriginalFilename() : "없음");

        // 서비스에 이미지 파일 전달
        ClothesDto created = clothesService.createClothes(request, image);

        log.info("의상 등록 성공: id = {}, ownerId = {}, type = {}",
            created.id(), created.ownerId(), created.type()
        );
        return ResponseEntity.status(201).body(created);
    }

    /**
     * 의상 목록 조회
     *
     * <p>Cursor 기반 페이지네이션과 타입 필터를 지원하는 의상 목록 조회 API</p>
     * <p>typeEqual은 {@link ClothesType} 중 하나여야 하며, {@link ClothesTypeValid}로 검증</p>
     *
     * @param ownerId   조회할 사용자의 ID (필수)
     * @param limit     조회할 최대 개수 (기본값 20)
     * @param cursor    생성일 기준 커서 (선택)
     * @param idAfter   UUID 기준 커서 (선택)
     * @param typeEqual 의상 타입 필터 (선택, {@link ClothesTypeValid})
     * @return {@link CursorPageResponse} 조회 결과
     */
    @GetMapping
    public CursorPageResponse<ClothesDto> getClothesList(
        @RequestParam UUID ownerId,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) Instant cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(required = false) @ClothesTypeValid ClothesType typeEqual
    ) {
        log.info("GET /api/clothes 요청 수신: ownerId = {}, limit = {}, cursor = {}, idAfter = {}, type = {}",
            ownerId, limit, cursor, idAfter, typeEqual
        );

        // 서비스 호출
        CursorPageResponse<ClothesDto> response =
            clothesService.getClothesList(ownerId, limit, cursor, idAfter, typeEqual);
        log.info("의상 목록 조회 완료: ownerId = {}, 반환 개수 = {}", ownerId, response.data().size());

        return response;
    }

    /**
     * 의상 수정
     *
     * <p>Multipart/form-data를 통해 의상 정보와 선택적으로 이미지를 업로드하여 수정
     * USER 또는 ADMIN 권한 필요
     *
     * @param clothesId 수정할 의상 ID
     * @param request 의상 수정 요청 DTO
     * @param image 업로드할 이미지 파일 (선택)
     * @return 수정된 의상 DTO
     */
    @PatchMapping("/{clothesId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ClothesDto> updateClothes(
        @PathVariable UUID clothesId,
        @RequestPart("request") ClothesUpdateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        log.info("PATCH /api/clothes/{} 요청: name={}, type={}, attributes={}",
            clothesId, request.name(), request.type(), request.attributes());

        ClothesDto updated = clothesService.updateClothes(clothesId, request, image);
        return ResponseEntity.ok(updated);
    }

    /**
     * 의상 삭제
     *
     * @param clothesId 삭제할 의상 ID
     * @return 204 No Content
     */
    @DeleteMapping("/{clothesId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Void> deleteClothes(@PathVariable UUID clothesId) {
        log.info("의상 삭제 요청 - clothesId: {}", clothesId);
        clothesService.deleteClothes(clothesId);

        log.info("의상 삭제 완료 - clothesId: {}", clothesId);
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }
}
