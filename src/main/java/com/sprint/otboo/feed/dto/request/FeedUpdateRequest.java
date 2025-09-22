package com.sprint.otboo.feed.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FeedUpdateRequest(
    @NotBlank String content
) {

}
