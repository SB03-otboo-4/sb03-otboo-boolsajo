package com.sprint.otboo.feed.dto.data;

import com.sprint.otboo.user.dto.data.AuthorDto;
import java.time.Instant;
import java.util.UUID;

public record CommentDto(
    UUID id,
    Instant createdAt,
    UUID feedId,
    AuthorDto author,
    String content
) {

}