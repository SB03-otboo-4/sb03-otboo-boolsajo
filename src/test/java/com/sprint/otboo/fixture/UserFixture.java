package com.sprint.otboo.fixture;


import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public class UserFixture {

    public static final String DEFAULT_EMAIL = "email+" + UUID.randomUUID() + "@example.com";
    public static final String DEFAULT_NAME = "홍길동";

    public static User createUserWithDefault() {
        return User.builder()
            .email(DEFAULT_EMAIL)
            .username(DEFAULT_NAME)
            .role(Role.USER)
            .profileImageUrl("profile.png")
            .createdAt(Instant.now())
            .build();
    }

    public static User createUserWithEmail(String email) {
        return User.builder()
            .email(email)
            .username(DEFAULT_NAME)
            .role(Role.USER)
            .profileImageUrl("profile.png")
            .createdAt(Instant.now())
            .build();
    }

    public static User create(UUID id, String username, String profileImageUrl) {
        return User.builder()
            .id(id)
            .username(username)
            .profileImageUrl(profileImageUrl)
            .build();
    }

    public static User create(String username, String profileImageUrl) {
        return User.builder()
            .username(username)
            .profileImageUrl(profileImageUrl)
            .build();
    }
}
