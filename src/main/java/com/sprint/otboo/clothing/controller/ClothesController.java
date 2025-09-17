package com.sprint.otboo.clothing.controller;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.service.ClothesService;
import com.sprint.otboo.clothing.valid.ClothesTypeValid;
import com.sprint.otboo.common.dto.CursorPageResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Clothes Controller
 * <p>의상 관련 REST API 요청을 처리하는 컨트롤러입니다.</p>
 *
 * <ul>
 *   <li>의상 등록</li>
 *   <li>의상 목록 조회</li>
 * </ul>
 *
 * <p>보안: 인증된 사용자( USER, ADMIN )만 접근 가능하도록 Spring Security 설정 필요</p>
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
     * <p>multipart/form-data 요청</p>
     * - image : 이미지 파일 (binary)
     * - request : JSON 문자열 (ClothesCreateRequest)
     */
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
     *
     * <ul>
     *   <li>ownerId: 특정 사용자의 의상만 조회</li>
     *   <li>limit: 조회할 최대 개수 (기본값 20)</li>
     *   <li>cursor, idAfter: 커서 기반 페이지네이션</li>
     *   <li>typeEqual: 의상 타입 필터</li>
     * </ul>
     *
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
        log.info("의상 목록 조회 완료: ownerId = {}, 반환 개수 = {}", ownerId, response.content().size());

        return response;
    }

}
