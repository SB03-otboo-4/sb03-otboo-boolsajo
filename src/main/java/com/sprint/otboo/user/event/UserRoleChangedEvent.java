package com.sprint.otboo.user.event;

import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;

public record UserRoleChangedEvent(
    User user,
    Role oldRole,
    Role newRole
) {

}
