package com.sprint.otboo.fixture;

import com.sprint.otboo.user.entity.User;

import java.util.UUID;

public class UserFixture {

    public static User create(UUID id, String username, String profileImageUrl) {
        return User.builder()
            .id(id)
            .username(username)
            .profileImageUrl(profileImageUrl)
            .build();
    }
}
