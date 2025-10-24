package com.sprint.otboo.follow.dto.data;

import java.util.UUID;

public record FollowDto(
    UUID id,
    UUID followerId,
    UUID followeeId
) {}
