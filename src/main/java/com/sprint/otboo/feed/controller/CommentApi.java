package com.sprint.otboo.feed.controller;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.feed.dto.request.CommentCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Comment", description = "Comment API")
public interface CommentApi {

    @Operation(summary = "Comment Create", description = "새로운 Comment를 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", description = "Comment 등록 성공",
            content = @Content(schema = @Schema(implementation = CommentDto.class))
        ),
        @ApiResponse(
            responseCode = "400", description = "잘못된 요청 본문",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    ResponseEntity<CommentDto> create(UUID feedId, CommentCreateRequest request);

    @Operation(
        summary = "Comment List",
        description = "커서 기반 페이지네이션으로 특정 Feed에 달린 Comment를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", description = "댓글 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = CursorPageResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", description = "잘못된 요청 파라미터",
            content = @Content(schema = @Schema(hidden = true))
        ),
        @ApiResponse(
            responseCode = "404", description = "Feed를 찾을 수 없음",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    ResponseEntity<CursorPageResponse<CommentDto>> getComments(
        @Parameter(description = "feedId")
        @PathVariable UUID feedId,
        @Parameter(description = "cursor")
        @RequestParam(required = false) String cursor,
        @Parameter(description = "idAfter")
        @RequestParam(required = false) UUID idAfter,
        @Parameter(description = "limit")
        @RequestParam int limit
    );
}
