package com.sprint.otboo.follow.controller;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.dto.ErrorResponse;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.dto.data.FollowSummaryDto;
import com.sprint.otboo.follow.dto.request.FollowCreateRequest;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "팔로우 관리", description = "팔로우 관련 API")
@RequestMapping("/api/follows")
@SecurityRequirement(name = "bearerAuth")
public interface FollowApi {

    @Operation(summary = "팔로우 생성", description = "인증 사용자(follower)가 요청 본문의 followeeId를 팔로우합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "생성 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = com.sprint.otboo.follow.dto.response.FollowCreateResponse.class)))
        ,
        @ApiResponse(responseCode = "400", description = "요청 값 오류",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요/형식 오류",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자 없음",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "이미 팔로우 관계",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<com.sprint.otboo.follow.dto.response.FollowCreateResponse>
    create(@Valid @RequestBody FollowCreateRequest request);

    @Operation(summary = "팔로우 요약 조회", description = "userId를 지정하면 그 사용자의 요약과 viewer 관점을 함께 반환")
    @ApiResponse(responseCode = "200",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = com.sprint.otboo.follow.dto.response.FollowSummaryResponse.class)))
    ResponseEntity<com.sprint.otboo.follow.dto.response.FollowSummaryResponse> getSummary(
        @RequestParam(value = "userId", required = false) UUID userId);

    @Operation(summary = "팔로잉 목록 조회", description = "특정 사용자가 팔로우하는 사용자 목록(팔로잉)을 커서 기반으로 조회")
    @ApiResponse(responseCode = "200",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = CursorPageResponse.class)))
    ResponseEntity<CursorPageResponse<FollowListItemResponse>> getFollowings(
        @Parameter(description = "대상 사용자 ID", required = true) @RequestParam("followerId") UUID followerId,
        @Parameter(description = "다음 페이지 커서(ISO-8601 Instant)", example = "2025-10-16T03:00:00Z")
        @RequestParam(value = "cursor", required = false) String cursor,
        @Parameter(description = "createdAt 동일시 타이브레이커(이전 응답의 nextIdAfter 사용)")
        @RequestParam(value = "idAfter", required = false) UUID idAfter,
        @Parameter(description = "페이지 크기(1~100). 기본 20", example = "20")
        @RequestParam(value = "limit", defaultValue = "20") int limit,
        @Parameter(description = "사용자 이름 부분 검색(대소문자 구분 없음)", example = "ali")
        @RequestParam(value = "nameLike", required = false) String nameLike
    );

    @Operation(summary = "팔로워 목록 조회", description = "인증 사용자를 팔로우하는 사용자 목록(팔로워)을 커서 기반으로 조회")
    @ApiResponse(responseCode = "200",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = CursorPageResponse.class)))
    ResponseEntity<CursorPageResponse<FollowListItemResponse>> getFollowers(
        @Parameter(description = "다음 페이지 커서(ISO-8601 Instant)", example = "2025-10-16T03:00:00Z")
        @RequestParam(value = "cursor", required = false) String cursor,
        @Parameter(description = "createdAt 동일시 타이브레이커(이전 응답의 nextIdAfter 사용)")
        @RequestParam(value = "idAfter", required = false) UUID idAfter,
        @Parameter(description = "페이지 크기(1~100). 기본 20", example = "20")
        @RequestParam(value = "limit", defaultValue = "20") int limit,
        @Parameter(description = "사용자 이름 부분 검색(대소문자 구분 없음)", example = "ali")
        @RequestParam(value = "nameLike", required = false) String nameLike
    );

    @Operation(summary = "언팔로우", description = "팔로우 관계 ID로 언팔로우")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "성공적으로 언팔로우됨"),
        @ApiResponse(responseCode = "401", description = "인증되지 않음"),
        @ApiResponse(responseCode = "404", description = "팔로우 관계가 존재하지 않음")
    })
    @DeleteMapping("/{followId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unfollow(@PathVariable UUID followId);
}
