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

    @Operation(
        summary = "팔로잉 목록 조회",
        description = "특정 사용자가 팔로우하고 있는(팔로잉) 사용자 목록을 커서 기반으로 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공",
        content = @Content(
            schema = @Schema(implementation = CursorPageResponse.class),
            examples = @ExampleObject(value = """
        {
          "data": [
            {
              "id": "f1d2d2f9-12ab-4c34-b5cc-bb1f2cd9c123",
              "followee": {
                "userId": "11111111-1111-1111-1111-111111111111",
                "name": "bob",
                "profileImageUrl": "https://.../bob.jpg"
              },
              "follower": {
                "userId": "68e17953-f79f-4d4f-8839-b26054887d5f",
                "name": "me",
                "profileImageUrl": "https://.../me.jpg"
              },
              "createdAt": "2025-10-14T05:29:40Z"
            }
          ],
          "nextCursor": "2025-10-14T05:29:40Z",
          "nextIdAfter": "f1d2d2f9-12ab-4c34-b5cc-bb1f2cd9c123",
          "hasNext": false,
          "totalCount": 2,
          "sortBy": "createdAt",
          "sortDirection": "DESCENDING"
        }
        """)
        )
    )
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (cursor 형식 오류 등)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "인증 필요/형식 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/followings")
    ResponseEntity<CursorPageResponse<FollowListItemResponse>> getFollowings(
        @RequestParam("followerId")
        @io.swagger.v3.oas.annotations.Parameter(
            description = "대상 사용자 ID (해당 사용자의 팔로잉 목록을 조회)"
        ) UUID followerId,
        @RequestParam(value = "cursor", required = false)
        @io.swagger.v3.oas.annotations.Parameter(
            description = "다음 페이지 커서(ISO-8601 Instant). 이전 응답의 nextCursor 사용"
        ) String cursor,
        @RequestParam(value = "idAfter", required = false)
        @io.swagger.v3.oas.annotations.Parameter(
            description = "createdAt 동일 시 타이브레이커. 이전 응답의 nextIdAfter 사용"
        ) UUID idAfter,
        @RequestParam(value = "limit", defaultValue = "20")
        @io.swagger.v3.oas.annotations.Parameter(
            description = "페이지 크기(1~100). 기본 20"
        ) int limit,
        @RequestParam(value = "nameLike", required = false)
        @io.swagger.v3.oas.annotations.Parameter(
            description = "사용자 이름 부분 검색(대소문자 구분 없음)"
        ) String nameLike
    );

    @Operation(
        summary = "팔로워 목록 조회",
        description = "인증 사용자를 팔로우하는 사용자 목록을 커서 기반으로 조회합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            schema = @Schema(implementation = CursorPageResponse.class),
            examples = @ExampleObject(value = """
        {
          "data": [
            {
              "id": "a4d5c2d6-8f77-4a2c-9d03-03d0d4b2ef11",
              "followee": null,
              "follower": {
                "userId": "2d1a2a1c-3e6f-4a40-9f31-3b2f4d5a66c1",
                "name": "alice",
                "profileImageUrl": "https://.../alice.png"
              },
              "createdAt": "2025-10-16T03:00:00Z"
            }
          ],
          "nextCursor": "2025-10-16T03:00:00Z",
          "nextIdAfter": "a4d5c2d6-8f77-4a2c-9d03-03d0d4b2ef11",
          "hasNext": true,
          "totalCount": 23,
          "sortBy": "createdAt",
          "sortDirection": "DESCENDING"
        }
        """)
        )
    )
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (cursor 형식 오류 등)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "인증 필요/형식 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/followers")
    ResponseEntity<CursorPageResponse<FollowListItemResponse>> getFollowers(
        @RequestParam(value = "cursor", required = false)
        @io.swagger.v3.oas.annotations.Parameter(
            description = "다음 페이지 커서(ISO-8601 Instant). 이전 응답의 nextCursor 사용"
        ) String cursor,
        @RequestParam(value = "idAfter", required = false)
        @io.swagger.v3.oas.annotations.Parameter(
            description = "createdAt 동일 시 타이브레이커. 이전 응답의 nextIdAfter 사용"
        ) UUID idAfter,
        @RequestParam(value = "limit", defaultValue = "20")
        @io.swagger.v3.oas.annotations.Parameter(
            description = "페이지 크기(1~100). 기본 20"
        ) int limit,
        @RequestParam(value = "nameLike", required = false)
        @io.swagger.v3.oas.annotations.Parameter(
            description = "사용자 이름 부분 검색(대소문자 구분 없음)"
        ) String nameLike
    );
}
