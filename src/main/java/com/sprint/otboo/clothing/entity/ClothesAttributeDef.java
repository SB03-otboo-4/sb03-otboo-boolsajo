package com.sprint.otboo.clothing.entity;

import com.sprint.otboo.common.base.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "clothes_attributes_def")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ClothesAttributeDef extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "select_values", length = 255)
    private String selectValues;

    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClothesAttribute> attributes = new ArrayList<>();
}
