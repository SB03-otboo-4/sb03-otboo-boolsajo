package com.sprint.otboo.feed.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "피드 관리", description = "피드 관련 API")
public interface LikeApi {

    @Operation(
        summary = "피드 좋아요",
        description = "피드 좋아요 API",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204", description = "피드 좋아요 성공", content = @Content
        ),
        @ApiResponse(
            responseCode = "401", description = "인증 실패", content = @Content
        ),
        @ApiResponse(
            responseCode = "404", description = "Feed 또는 User를 찾을 수 없음", content = @Content
        )
    })
    ResponseEntity<Void> like(
        @Parameter(description = "feedId", required = true)
        @PathVariable UUID feedId,

        @Parameter(hidden = true)
        CustomUserDetails user
    );
    @Operation(
        summary = "피드 좋아요 취소",
        description = "피드 좋아요 취소 API",
        security = @SecurityRequirement(name = "bearerAuth"),
        operationId = "removeFeedLike"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204", description = "피드 좋아요 취소 성공", content = @Content
        ),
        @ApiResponse(
            responseCode = "401", description = "인증 실패", content = @Content
        ),
        @ApiResponse(
            responseCode = "404", description = "Feed 또는 User를 찾을 수 없음", content = @Content
        )
    })
    ResponseEntity<Void> removeLike(
        @Parameter(description = "feedId", required = true)
        @PathVariable UUID feedId,

        @Parameter(hidden = true)
        CustomUserDetails principal
    );
}
