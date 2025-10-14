package com.sprint.otboo.feedsearch.dto;

import java.time.Instant;
import java.util.UUID;

public record CursorDto(Instant updatedAt, UUID id) {

    public static CursorDto epoch() {
        return new CursorDto(Instant.EPOCH, new UUID(0, 0));
    }
}
