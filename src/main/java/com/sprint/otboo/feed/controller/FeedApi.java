package com.sprint.otboo.feed.controller;

import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

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
}
