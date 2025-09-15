package com.sprint.otboo.clothing.entity;

import com.sprint.otboo.common.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 의상(Clothes) 엔티티
 *
 * <p>사용자 소유 의상 정보를 관리
 *
 * <ul>
 *   <li>ownerId : 의상을 소유한 사용자 ID</li>
 *   <li>name : 의상 이름</li>
 *   <li>imageUrl : 의상 이미지 URL</li>
 *   <li>type: 의상 종류( ClothesType enum )</li>
 *   <li>BaseUpdatableEntity 상속( id, createdAt, updatedAt 관리 )</li>
 * </ul>
 */
@Getter
@Entity
@Table(name = "clothes")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    /**
     * 팩토리 메서드
     *
     * @param ownerId 소유자 ID
     * @param name 의상 이름
     * @param imageUrl 의상 이미지 URL
     * @param type 의상 종류
     * @return 생성된 Clothes 엔티티
     */
    public static Clothes create(UUID ownerId, String name, String imageUrl, ClothesType type) {
        return Clothes.builder()
            .ownerId(ownerId)
            .name(name)
            .imageUrl(imageUrl)
            .type(type)
            .build();
    }
}