package com.sprint.otboo.follow.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "FollowSummaryResponse", description = "팔로우 요약(대상 사용자 + 로그인 사용자 관점)")
public record FollowSummaryResponse(
    @Schema(description = "요약 대상(팔로우 조회 대상) 사용자 ID")
    UUID followeeId,
    @Schema(description = "대상 사용자의 팔로워 수")
    long followerCount,
    @Schema(description = "대상 사용자의 팔로잉 수")
    long followingCount,
    @Schema(description = "로그인 사용자가 대상 사용자를 팔로우 중인지")
    boolean followedByMe,
    @Schema(description = "팔로우 중이라면 팔로우 관계 ID", nullable = true)
    UUID followedByMeId,
    @Schema(description = "대상 사용자가 로그인 사용자를 팔로우 중인지")
    boolean followingMe
) {}
