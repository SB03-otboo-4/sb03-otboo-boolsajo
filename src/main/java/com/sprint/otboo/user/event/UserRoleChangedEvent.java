package com.sprint.otboo.user.event;

import com.sprint.otboo.user.entity.Role;
import java.util.UUID;

public record UserRoleChangedEvent(
    UUID userId,
    Role previousRole,
    Role newRole
) {
}

