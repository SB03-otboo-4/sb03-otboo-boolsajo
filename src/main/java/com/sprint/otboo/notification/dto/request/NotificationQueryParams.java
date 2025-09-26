package com.sprint.otboo.notification.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record NotificationQueryParams(
    String cursor,
    UUID idAfter,
    @Positive @Max(50) int limit
) {
    public Instant parsedCursor() {
        return Optional.ofNullable(cursor)
            .filter(s -> !s.isBlank())
            .map(Instant::parse)
            .orElse(null);
    }

    /**
     * 다음 페이지 여부 판별용
     * */
    public int fetchSize() {
        return Math.min(limit + 1, 51);
    }
}
