package com.sprint.otboo.follow.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "팔로우 생성 요청")
public record FollowCreateRequest(
    @Schema(description = "팔로우 대상 사용자 ID", example = "2b6f5a3d-3b4a-4e0c-9a77-3c4a0f9a2d00")
    @NotNull UUID followeeId
) {}
