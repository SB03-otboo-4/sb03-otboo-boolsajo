package com.sprint.otboo.user.dto.data;

import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import java.time.Instant;
import java.util.UUID;

public record UserDto(
    UUID id,
    Instant createdAt,
    String email,
    String name,
    Role role,
    LoginType linkedOAuthProviders,
    Boolean locked,
    String profileImageUrl,
    String providerId,
    Instant updatedAt
) {

}