package com.sprint.otboo.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "UserSummaryResponse", description = "사용자 요약 정보")
public record UserSummaryResponse(
    @Schema(description = "사용자 ID", example = "2d1a2a1c-3e6f-4a40-9f31-3b2f4d5a66c1")
    UUID userId,

    @Schema(description = "사용자 이름", example = "alice")
    String name,

    @Schema(description = "프로필 이미지 URL", nullable = true,
        example = "https://cdn.example.com/profiles/alice.png")
    String profileImageUrl
) {}
