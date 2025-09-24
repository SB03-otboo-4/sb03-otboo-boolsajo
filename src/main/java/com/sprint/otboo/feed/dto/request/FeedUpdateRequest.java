package com.sprint.otboo.feed.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedUpdateRequest(
    @NotBlank(message = "content는 비어 있을 수 없습니다.")
    @Size(max = 1000, message = "content는 최대 1000자까지 가능합니다.")
    String content
) {

}
