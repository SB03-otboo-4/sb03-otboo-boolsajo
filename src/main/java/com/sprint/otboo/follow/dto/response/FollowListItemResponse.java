package com.sprint.otboo.follow.dto.response;

import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "FollowListItemResponse", description = "팔로우 관계 목록 아이템")
public record FollowListItemResponse(
    @Schema(description = "팔로우 관계 ID", example = "a4d5c2d6-8f77-4a2c-9d03-03d0d4b2ef11")
    UUID id,

    @Schema(description = "피팔로우(내가 팔로잉하는 대상) 요약. 팔로워 목록에서는 null일 수 있음", nullable = true)
    UserSummaryResponse followee,

    @Schema(description = "팔로워(나를 팔로우하는 사용자) 요약. 팔로잉 목록에서는 나 자신", nullable = true)
    UserSummaryResponse follower,

    @Schema(description = "팔로우 생성 시각(ISO-8601)", example = "2025-10-16T03:00:00Z")
    Instant createdAt
) {}
