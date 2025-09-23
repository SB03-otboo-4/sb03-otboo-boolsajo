package com.sprint.otboo.feed.controller;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.feed.dto.request.FeedUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Feed", description = "Feed API")
public interface FeedApi {

    @Operation(summary = "Feed Create", description = "새로운 Feed를 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", description = "Feed 등록 성공",
            content = @Content(schema = @Schema(implementation = FeedDto.class))
        ),
        @ApiResponse(
            responseCode = "400", description = "잘못된 요청 본문",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    ResponseEntity<FeedDto> create(FeedCreateRequest request);

    @Operation(
        summary = "Feed List",
        description = "커서 기반 페이지네이션으로 Feed 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CursorPageResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", description = "잘못된 요청 파라미터",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    ResponseEntity<CursorPageResponse<FeedDto>> getFeeds(
        @Parameter(description = "커서")
        @RequestParam(required = false) String cursor,

        @Parameter(description = "idAfter")
        @RequestParam(required = false) UUID idAfter,

        @Parameter(description = "피드 총갯수")
        @RequestParam int limit,

        @Parameter(description = "정렬 컬럼(createdAt|likeCount)")
        @RequestParam String sortBy,

        @Parameter(description = "정렬 방향(ASCENDING|DESCENDING)")
        @RequestParam String sortDirection,

        @Parameter(description = "제목/내용 키워드 부분일치")
        @RequestParam(required = false) String keywordLike,

        @Parameter(description = "하늘 상태 필터")
        @RequestParam(name = "skyStatusEqual", required = false) SkyStatus skyStatusEqual,

        @Parameter(description = "강수 형태 필터")
        @RequestParam(name = "precipitationTypeEqual", required = false) PrecipitationType precipitationTypeEqual,

        @Parameter(description = "작성자 ID 필터(UUID)")
        @RequestParam(name = "authorIdEqual", required = false) UUID authorIdEqual
    );

    @Operation(summary = "Feed Update", description = "기존 Feed의 내용을 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", description = "Feed 수정 성공",
            content = @Content(schema = @Schema(implementation = FeedDto.class))
        ),
        @ApiResponse(
            responseCode = "400", description = "잘못된 요청 본",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401", description = "인증되지 않음",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403", description = "수정 권한 없음(작성자 불일치)",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404", description = "작성자 또는 Feed를 찾을 수 없음",
            content = @Content
        )
    })
    ResponseEntity<FeedDto> update(
        @Parameter(description = "수정할 Feed ID", required = true)
        UUID feedId,
        @Parameter(hidden = true)
        CustomUserDetails user,
        @RequestBody(
            required = true,
            description = "수정 요청 바디(content)",
            content = @Content(schema = @Schema(implementation = FeedUpdateRequest.class))
        )
        @Valid FeedUpdateRequest request
    );
    @Operation(summary = "Feed Delete", description = "피드를 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않음"),
        @ApiResponse(responseCode = "403", description = "삭제 권한 없음(작성자 불일치)"),
        @ApiResponse(responseCode = "404", description = "작성자 또는 Feed를 찾을 수 없음")
    })
    ResponseEntity<Void> delete(
        @Parameter(description = "삭제할 Feed ID", required = true) UUID feedId,
        @Parameter(hidden = true) CustomUserDetails user
    );
}
