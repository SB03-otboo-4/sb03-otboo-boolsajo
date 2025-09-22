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

@Tag(name = "Like", description = "Feed Like API")
public interface LikeApi {

    @Operation(
        summary = "Feed Like",
        description = "로그인 사용자가 해당 Feed에 좋아요를 등록합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204", description = "좋아요 등록 성공 (No Content)", content = @Content
        ),
        @ApiResponse(
            responseCode = "401", description = "인증 실패", content = @Content
        ),
        @ApiResponse(
            responseCode = "404", description = "Feed 또는 User를 찾을 수 없음", content = @Content
        )
    })
    ResponseEntity<Void> like(
        @Parameter(description = "좋아요 대상 Feed ID", required = true)
        @PathVariable UUID feedId,

        @Parameter(hidden = true)
        CustomUserDetails user
    );
}
