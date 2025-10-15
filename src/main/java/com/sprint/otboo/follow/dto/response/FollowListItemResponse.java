package com.sprint.otboo.follow.dto.response;

import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public record FollowListItemResponse(
    UUID id,
    UserSummaryResponse followee,
    UserSummaryResponse follower,
    Instant createdAt
) {}
