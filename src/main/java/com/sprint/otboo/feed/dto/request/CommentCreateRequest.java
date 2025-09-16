package com.sprint.otboo.feed.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CommentCreateRequest(
    @NotNull UUID feedId,
    @NotNull UUID authorId,
    @NotBlank String content
) {

}
