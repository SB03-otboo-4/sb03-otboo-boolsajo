package com.sprint.otboo.follow.dto.response;

import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "FollowCreateResponse", description = "팔로우 생성 응답")
public record FollowCreateResponse(
    @Schema(description = "팔로우 관계 ID")
    UUID id,
    @Schema(description = "피팔로우 요약")
    UserSummaryResponse followee,
    @Schema(description = "팔로워 요약")
    UserSummaryResponse follower
) {}