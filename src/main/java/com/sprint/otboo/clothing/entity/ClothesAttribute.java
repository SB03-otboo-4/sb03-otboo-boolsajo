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

    // 팩토리 메서드
    public static ClothesAttribute create(UUID clothesId, UUID definitionId, String value) {
        return ClothesAttribute.builder()
            .clothesId(clothesId)
            .definitionId(definitionId)
            .value(value)
            .build();
    }
}