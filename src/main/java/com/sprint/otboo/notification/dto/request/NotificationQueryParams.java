package com.sprint.otboo.notification.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record NotificationQueryParams(
    String cursor,
    UUID idAfter,
    @Positive @Max(50) int limit
) {

}
