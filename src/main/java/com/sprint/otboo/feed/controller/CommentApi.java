package com.sprint.otboo.feed.controller;

import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.feed.dto.request.CommentCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

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
}