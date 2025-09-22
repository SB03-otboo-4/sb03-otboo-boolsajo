package com.sprint.otboo.clothing.entity;

import com.sprint.otboo.common.base.BaseUpdatableEntity;
import com.sprint.otboo.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 의상(Clothes) 엔티티
 *
 * <p>사용자가 소유한 의상 정보를 관리
 *
 * <ul>
 *   <li>user : 의상을 소유한 사용자 엔티티</li>
 *   <li>name : 의상 이름</li>
 *   <li>imageUrl : 의상 이미지 URL</li>
 *   <li>type : 의상 종류(ClothesType enum)</li>
 *   <li>attributes : 의상 속성 리스트(ClothesAttribute 엔티티)</li>
 *   <li>BaseUpdatableEntity 상속 : id, createdAt, updatedAt 관리</li>
 * </ul>
 */
@Getter
@Entity
@Table(name = "clothes")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clothes extends BaseUpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_clothes_user"))
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

    /**
     * 팩토리 메서드로 Clothes 엔티티 생성
     *
     * @param user 의상을 소유한 사용자 엔티티
     * @param name 의상 이름
     * @param imageUrl 의상 이미지 URL
     * @param type 의상 종류
     * @param createdAt 의상 생성 시각
     * @return 새로 생성된 Clothes 엔티티
     */
    public static Clothes create(User user, String name, String imageUrl, ClothesType type, Instant createdAt) {
        return Clothes.builder()
            .user(user)
            .name(name)
            .imageUrl(imageUrl)
            .type(type)
            .createdAt(createdAt)
            .build();
    }
}