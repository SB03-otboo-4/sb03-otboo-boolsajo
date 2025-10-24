package com.sprint.otboo.clothing.controller.api;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesUpdateRequest;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.valid.ClothesTypeValid;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "의상 관리", description = "의상 관련 API")
@RequestMapping("/api/clothes")
public interface ClothesApi {

    // 1. 의상 목록 조회
    @Operation(summary = "옷 목록 조회", description = "Cursor 기반 페이지네이션과 타입 필터를 지원하는 의상 목록 조회 API")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CursorPageResponse.class),
                examples = @ExampleObject(value = """
                {
                  "data": [
                    {
                      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                      "ownerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                      "name": "셔츠",
                      "imageUrl": "http://example.com/image.png",
                      "type": "TOP",
                      "attributes": [
                        {
                          "definitionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                          "definitionName": "색상",
                          "selectableValues": ["빨강", "파랑"],
                          "value": "빨강"
                        }
                      ]
                    }
                  ],
                  "nextCursor": "string",
                  "nextIdAfter": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "hasNext": true,
                  "totalCount": 100,
                  "sortBy": "createdAt",
                  "sortDirection": "ASCENDING"
                }
                """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "조회 실패",
            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class)))
    })
    CursorPageResponse<ClothesDto> getClothesList(
        @Parameter(description = "조회할 사용자의 ID", required = true) @RequestParam UUID ownerId,
        @Parameter(description = "조회할 최대 개수", required = false) @RequestParam(defaultValue = "20") int limit,
        @Parameter(description = "생성일 기준 커서", required = false) @RequestParam(required = false) Instant cursor,
        @Parameter(description = "UUID 기준 커서", required = false) @RequestParam(required = false) UUID idAfter,
        @Parameter(description = "의상 타입 필터", required = false) @RequestParam(required = false) @ClothesTypeValid ClothesType typeEqual
    );

    // 2. 의상 등록
    @Operation(summary = "옷 등록", description = "새로운 의상을 등록합니다. multipart/form-data 형식을 사용하며, image 또는 imageUrl을 선택적으로 전달할 수 있습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "등록 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ClothesDto.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "등록 실패",
            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ClothesDto> createClothes(
        @Parameter(description = "의상 등록 요청 DTO", required = true) @RequestPart("request") ClothesCreateRequest request,
        @Parameter(description = "업로드할 이미지 파일", required = false) @RequestPart(value = "image", required = false) MultipartFile image,
        @Parameter(description = "외부 이미지 URL", required = false) @RequestPart(value = "imageUrl", required = false) String imageUrl
    );

    // 3. 의상 수정
    @Operation(summary = "옷 수정", description = "Multipart/form-data를 통해 의상 정보와 선택적으로 이미지를 업로드하여 수정")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClothesDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "수정 실패",
            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ClothesDto> updateClothes(
        @Parameter(description = "수정할 의상 ID", required = true, in = ParameterIn.PATH) @PathVariable UUID clothesId,
        @Parameter(description = "의상 수정 요청 DTO", required = true) @RequestPart("request") ClothesUpdateRequest request,
        @Parameter(description = "업로드할 이미지 파일", required = false) @RequestPart(value = "image", required = false) MultipartFile image
    );

    // 4. 의상 삭제
    @Operation(summary = "옷 삭제", description = "지정한 ID의 의상을 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "삭제 실패",
            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteClothes(
        @Parameter(description = "삭제할 의상 ID", required = true, in = ParameterIn.PATH) @PathVariable UUID clothesId
    );

    // 5. URL 기반 의상 정보 추출
    @Operation(summary = "구매 링크로 옷 정보 불러오기", description = "주어진 상품 URL에서 의상 정보를 추출합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ClothesDto.class))),
        @ApiResponse(responseCode = "400", description = "조회 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ClothesDto extractByUrl(
        @Parameter(description = "상품 상세 페이지 URL", required = true) @RequestParam String url
    );
}