package com.sprint.otboo.clothing.controller;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.service.ClothesService;
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
 * </ul>
 *
 * <p>보안: 인증된 사용자만 접근 가능하도록 Spring Security 설정 필요</p>
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

        log.info("의상 등록 성공: id={}", created.id());
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping
    public CursorPageResponse<ClothesDto> getClothesList(
        @RequestParam UUID ownerId,
        @RequestParam int limit,
        @RequestParam(required = false) Instant cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(name = "typeEqual", required = false) ClothesType typeEqual
    ) {
        return clothesService.getClothesList(ownerId, limit, cursor, idAfter, typeEqual);
    }

}
