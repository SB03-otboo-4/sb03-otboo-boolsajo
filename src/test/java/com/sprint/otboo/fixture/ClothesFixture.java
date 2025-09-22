package com.sprint.otboo.fixture;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.user.entity.User;
import java.util.UUID;

public class ClothesFixture {

    public static Clothes create(UUID id, User user, String name, String imageUrl,
        ClothesType type) {
        return Clothes.builder()
            .id(id)
            .user(user)
            .name(name)
            .imageUrl(imageUrl)
            .type(type)
            .build();
    }
}
