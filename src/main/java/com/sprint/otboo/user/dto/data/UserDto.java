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

    /**
     * Oauth 제공자 목록을 List 형태로 반환하는 편의 메서드
     * */
    public java.util.List<String> getLinkedOAuthProvidersList() {
        return linkedOAuthProviders == LoginType.GENERAL
            ? java.util.List.of()
            : java.util.List.of(linkedOAuthProviders.name().toLowerCase());
    }
}