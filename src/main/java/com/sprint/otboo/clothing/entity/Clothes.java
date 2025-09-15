package com.sprint.otboo.clothing.entity;


import com.sprint.otboo.common.base.BaseUpdatableEntity;
import com.sprint.otboo.feed.entity.FeedClothes;
import com.sprint.otboo.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "clothes")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Clothes extends BaseUpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_clothes_user"))
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClothesType type;

    @Builder.Default
    @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClothesAttribute> attributes = new ArrayList<>();

    @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedClothes> feedClothes = new ArrayList<>();
}
