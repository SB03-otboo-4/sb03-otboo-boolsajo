package com.sprint.otboo.clothing.entity;

import com.sprint.otboo.common.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 의상 속성(ClothesAttribute) 엔티티
 *
 * <p>Clothes 엔티티에 속한 개별 속성을 관리
 *
 * <ul>
 *   <li>clothesId: 속성이 속한 의상 ID</li>
 *   <li>definitionId: 속성 정의 ID</li>
 *   <li>value: 속성 값</li>
 *   <li>BaseUpdatableEntity 상속( id, createdAt, updatedAt 관리 )</li>
 * </ul>
 */
@Getter
@Entity
@Table(name = "clothes_attribute")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesAttribute extends BaseUpdatableEntity {

    @Column(name = "clothes_id", nullable = false, columnDefinition = "uuid")
    private UUID clothesId;

    @Column(name = "definition_id", nullable = false, columnDefinition = "uuid")
    private UUID definitionId;

    @Column(name = "value", length = 100, nullable = false)
    private String value;

    /**
     * 팩토리 메서드
     *
     * @param clothesId 속성이 속한 의상 ID
     * @param definitionId 속성 정의 ID
     * @param value 속성 값
     * @return 생성된 ClothesAttribute 엔티티
     */
    public static ClothesAttribute create(UUID clothesId, UUID definitionId, String value) {
        return ClothesAttribute.builder()
            .clothesId(clothesId)
            .definitionId(definitionId)
            .value(value)
            .build();
    }
}