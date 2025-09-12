package com.sprint.otboo.user.entity;

import com.sprint.otboo.common.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseUpdatableEntity {

    @Column(name = "name", nullable = false, unique = true, length = 20)
    private String username;

    @Column(name = "password", length = 20)
    private String password;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role;

    @Column(name = "locked", nullable = false)
    private Boolean locked;

    @Column(name = "profile_image_url", length = 100)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private LoginType provider;

    @Column(name = "provider_id", length = 20, nullable = false)
    private String providerId;

}