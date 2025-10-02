package com.sprint.otboo.follow.dto.reqeust;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FollowCreateRequest(
    @NotNull UUID followerId,
    @NotNull UUID followeeId
) {}
