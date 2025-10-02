package com.sprint.otboo.follow.dto.reqeust;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FollowCreateRequest(
    @NotNull(message = "followerId는 필수입니다.") UUID followerId,
    @NotNull(message = "followeeId는 필수입니다.") UUID followeeId
) {}
