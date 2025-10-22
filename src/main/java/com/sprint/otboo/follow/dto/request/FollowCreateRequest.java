package com.sprint.otboo.follow.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FollowCreateRequest(
    @NotNull(message = "followeeId는 필수입니다.") UUID followeeId
) {}
