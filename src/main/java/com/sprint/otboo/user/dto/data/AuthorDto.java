package com.sprint.otboo.user.dto.data;

import java.util.UUID;

public record AuthorDto(
    UUID userId,
    String name,
    String profileImageUrl
) {

}
