package com.sprint.otboo.user.event;

import java.util.UUID;

public record UserLockedEvent(
    UUID userId
) {
}
