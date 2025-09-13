package com.sprint.otboo.clothing.entity;

import com.sprint.otboo.common.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "clothes_attribute")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ClothesAttribute extends BaseUpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothes_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_clothes_attr_clothes"))
    private Clothes clothes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_clothes_attr_def"))
    private ClothesAttributeDef definition;

    @Column(nullable = false, length = 100)
    private String value;
}
