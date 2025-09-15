package com.sprint.otboo.clothing.entity;

import com.sprint.otboo.common.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@Table(name = "clothes")
@SuperBuilder
@NoArgsConstructor
public class Clothes extends BaseUpdatableEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID ownerId;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private ClothesType type;

    // 팩토리 메서드
    public static Clothes create(UUID ownerId, String name, String imageUrl, ClothesType type) {
        return Clothes.builder()
            .ownerId(ownerId)
            .name(name)
            .imageUrl(imageUrl)
            .type(type)
            .build();
    }
}