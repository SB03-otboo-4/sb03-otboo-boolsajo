package com.sprint.otboo.clothing.entity;

import com.sprint.otboo.common.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 의상 속성(ClothesAttribute) 엔티티
 *
 * <p>의상(Clothes) 엔티티에 속한 개별 속성을 관리
 *
 * <ul>
 *   <li>clothes : 속성이 속한 의상 엔티티</li>
 *   <li>definition : 속성 정의 엔티티(ClothesAttributeDef)</li>
 *   <li>value : 속성 값</li>
 *   <li>BaseUpdatableEntity 상속 : id, createdAt, updatedAt 관리</li>
 * </ul>
 */
@Getter
@Entity
@Table(name = "clothes_attribute")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesAttribute extends BaseUpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothes_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_clothes_attr_clothes"))
    private Clothes clothes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false, foreignKey = @ForeignKey(name = "fk_clothes_attr_def"))
    private ClothesAttributeDef definition;

    @Column(name = "value", length = 100, nullable = false)
    private String value;

    /**
     * 팩토리 메서드로 ClothesAttribute 엔티티 생성
     *
     * @param clothes 속성이 속한 의상 엔티티
     * @param definition 속성 정의 엔티티
     * @param value 속성 값
     * @return 새로 생성된 ClothesAttribute 엔티티
     */
    public static ClothesAttribute create(Clothes clothes, ClothesAttributeDef definition, String value) {
        return ClothesAttribute.builder()
            .clothes(clothes)
            .definition(definition)
            .value(value)
            .build();
    }
}