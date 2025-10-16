package com.sprint.otboo.follow.controller;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.dto.ErrorResponse;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.dto.data.FollowSummaryDto;
import com.sprint.otboo.follow.dto.request.FollowCreateRequest;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "팔로우 관리", description = "팔로우 관련 API")
@RequestMapping("/api/follows")
@SecurityRequirement(name = "bearerAuth")
public interface FollowApi {

    @Operation(
        summary = "팔로우 생성",
        description = "인증 사용자(follower)가 요청 본문에 전달된 followeeId를 팔로우합니다."
    )
    @ApiResponse(responseCode = "201", description = "생성 성공",
        content = @Content(schema = @Schema(implementation = FollowDto.class),
            examples = @ExampleObject(value = """
            { "id":"0f3b8f54-1a9e-4b3d-9e50-3c2a0f3e7a9c",
              "followerId":"550e8400-e29b-41d4-a716-446655440000",
              "followeeId":"550e8400-e29b-41d4-a716-446655440001" }
            """)))
    @ApiResponse(responseCode = "400", description = "요청 값 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "인증 필요/형식 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "사용자 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "이미 팔로우 관계",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("")
    ResponseEntity<FollowDto> create(@RequestBody FollowCreateRequest request);

    @Operation(
        summary = "팔로우 요약 조회",
        description = "인증 사용자 기준으로 팔로워/팔로잉 수를 반환합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            schema = @Schema(implementation = FollowSummaryDto.class),
            examples = @ExampleObject(
                value = """
                {
                  "followerCount": 12,
                  "followingCount": 7
                }
                """
            )
        )
    )
    @ApiResponse(responseCode = "401", description = "인증 필요/형식 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/summary")
    ResponseEntity<FollowSummaryDto> getSummary();

    @Operation(summary = "팔로잉 목록 조회", description = "팔로잉 목록 조회 API")
    @ApiResponse(responseCode = "200", description = "성공",
        content = @Content(schema = @Schema(implementation = CursorPageResponse.class)))
    @ApiResponse(responseCode = "400", description = "실패",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/followings")
    ResponseEntity<CursorPageResponse<FollowListItemResponse>> getFollowings(
        @RequestParam("followerId") UUID followerId,
        @RequestParam(value = "cursor", required = false) String cursor,
        @RequestParam(value = "idAfter", required = false) UUID idAfter,
        @RequestParam("limit") int limit,
        @RequestParam(value = "nameLike", required = false) String nameLike
    );
}
