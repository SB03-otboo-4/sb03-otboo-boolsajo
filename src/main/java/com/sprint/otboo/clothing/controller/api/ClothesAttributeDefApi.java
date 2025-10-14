package com.sprint.otboo.clothing.controller.api;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefUpdateRequest;
import com.sprint.otboo.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "의상 속성 정의", description = "의상 속성 정의 관련 API")
@RequestMapping("/api/clothes/attribute-defs")
public interface ClothesAttributeDefApi {

    // 1. 목록 조회
    @Operation(summary = "의상 속성 정의 목록 조회", description = "의상 속성 정의 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ClothesAttributeDefDto.class)),
                examples = @ExampleObject(value = """
                [
                  {
                    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    "name": "색상",
                    "selectableValues": ["빨강", "파랑"],
                    "createdAt": "2025-10-01T09:12:55.850Z"
                  }
                ]
                """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "조회 실패",
            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<ClothesAttributeDefDto>> getAttributeDefs(
        @Parameter(description = "정렬 기준 (createdAt, name)", required = true) @RequestParam String sortBy,
        @Parameter(description = "정렬 방향 (ASCENDING, DESCENDING)", required = true) @RequestParam String sortDirection,
        @Parameter(description = "키워드 검색", required = false) @RequestParam(required = false) String keywordLike
    );

    // 2. 등록
    @Operation(summary = "의상 속성 정의 등록", description = "새로운 의상 속성 정의를 등록합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "등록 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ClothesAttributeDefDto.class),
                examples = @ExampleObject(value = """
                {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "name": "색상",
                  "selectableValues": ["빨강", "파랑"],
                  "createdAt": "2025-10-01T09:12:55.852Z"
                }
                """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "등록 실패",
            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ClothesAttributeDefDto> createAttributeDef(
        @Parameter(description = "등록 요청 바디", required = true) @RequestBody ClothesAttributeDefCreateRequest request
    );

    // 3. 삭제
    @Operation(summary = "의상 속성 정의 삭제", description = "지정한 ID의 의상 속성 정의를 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "삭제 실패",
            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteAttributeDef(
        @Parameter(description = "삭제할 속성 정의 ID", required = true, in = ParameterIn.PATH) @PathVariable UUID definitionId
    );

    // 4. 수정
    @Operation(summary = "의상 속성 정의 수정", description = "지정한 ID의 의상 속성 정의를 수정합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ClothesAttributeDefDto.class),
                examples = @ExampleObject(value = """
                {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "name": "색상",
                  "selectableValues": ["빨강", "파랑", "노랑"],
                  "createdAt": "2025-10-01T09:12:55.857Z"
                }
                """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "수정 실패",
            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ClothesAttributeDefDto> updateAttributeDef(
        @Parameter(description = "수정할 속성 정의 ID", required = true, in = ParameterIn.PATH) @PathVariable UUID definitionId,
        @Parameter(description = "수정 요청 바디", required = true) @RequestBody ClothesAttributeDefUpdateRequest request
    );
}