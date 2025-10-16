package com.sprint.otboo.user.dto.response;

import java.util.UUID;

public record UserSummaryResponse(
    UUID userId,
    String name,
    String profileImageUrl
) {}
