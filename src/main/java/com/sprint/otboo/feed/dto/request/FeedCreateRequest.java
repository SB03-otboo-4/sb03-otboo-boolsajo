package com.sprint.otboo.feed.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record FeedCreateRequest(
    @NotNull UUID authorId,
    @NotNull UUID weatherId,
    @NotEmpty List<UUID> clothesIds,
    @NotBlank String content
) {

}
